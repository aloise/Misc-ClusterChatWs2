package actors

import java.util.Date

import akka.util.Timeout
import models.BusinessCatalystOAuth._
import controllers.helpers.JsonResponses._
import models.MessageFromTypes.MessageFromType
import models.{ChatRoomMessage, MessageFromTypes, ChatRoomUserFeedback}
import models.base.Collection.ObjId
import play.api.{Play, Logger}
import play.api.Play.current
import akka.actor._
import play.sockjs.api.SockJS
import play.api.libs.json._
import reactivemongo.bson.BSONObjectID
import reactivemongo.core.commands.LastError
import utils.FutureUtils
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration._
import play.api.libs.concurrent.Execution.Implicits._
import play.modules.reactivemongo.json._
import play.modules.reactivemongo.json.BSONFormats.BSONObjectIDFormat
import play.modules.reactivemongo.json.collection._
import scala.util.Try

/**
 * User: aloise
 * Date: 15.10.14
 * Time: 22:47
 */
class ChatRoom( var chatRoom: models.ChatRoom, companyActor: ActorRef ) extends Actor {

  import ChatRoom._
  import models.ChatRoomHelper._
  import actors.messages.SocksMessages._
  import play.modules.reactivemongo.json.BSONFormats._
  import models.ChatRooms.{ jsonFormat => j6 }

  protected val userReconnectTimeout = Play.configuration.getMilliseconds( "chat_room.user_timeout" ).getOrElse( 60000L ).milliseconds

  var chatRoomCreator = chatRoom.user

  var user:Option[( ActorRef, models.User) ] = None

  var userDisconnectTimeout:Option[Cancellable] = None

  var assistants: Map[ ObjId, (models.Assistant, Option[ActorRef])] = Map()

  var messages:Seq[ChatRoomMessage] = chatRoom.messages

  var messageSequenceNum:Int = ( chatRoom.messages.map( _.sequenceNum ) :+ -1 ).max + 1

  // all assistants that were connected to the chat
  var allAssistants : Map[ObjId, models.Assistant] = Map()

  def receive:Receive = {

    case UpdateChatRoomData( c ) =>
      chatRoom = c

    case UserChatRoomJoin( newSocksActor, userData ) =>
      // unwatch old user
      user.foreach{ case ( actor, _ ) =>
        actor ! Error( "actors.ChatRoom.Exception", "Reconnecting" )
        context.unwatch( actor )
        actor ! PoisonPill
      }

      // watch for the new user
      user = Some( ( newSocksActor, userData ) )
      context.watch( newSocksActor )

      saveSystemChatMessage( models.ChatRooms.MessageType.UserJoin, Json.obj( "userId" -> userData._id ) )

      // cancel the await timeout
      userDisconnectTimeout.foreach{
        _.cancel()
      }
      userDisconnectTimeout = None

      // notify user
      sender ! UserChatRoomJoinedSuccessfully( self, getChatRoomData() )


    case a@AssistantInfoUpdated( newAssistantInfo ) =>
      user.foreach{ case ( actor, _ ) =>
        actor ! a
      }

      assistants.get( newAssistantInfo._id ).foreach{
        case ( _, actorOpt ) =>
          assistants = assistants.updated( newAssistantInfo._id,  ( newAssistantInfo, actorOpt ) )
      }

      allAssistants = allAssistants.updated( newAssistantInfo._id, newAssistantInfo )

    case ChatRoomAssistantJoin( chatRoomId, assistant, assistantActor ) if chatRoomId == chatRoom._id.stringify  =>

      assistants = assistants + ( assistant._id -> ( assistant, Some(assistantActor) ) )
      allAssistants = allAssistants + ( assistant._id -> assistant )

      saveSystemChatMessage( models.ChatRooms.MessageType.AssistantJoin, Json.obj( "assistantId" -> assistant._id ) )

      user.foreach{ case ( userActor, _ ) =>
        userActor ! UserAssistantChatRoomJoin( assistant._id.stringify, assistant.displayName, assistant.avatar )
      }

    // reconnect the assistant disconnected earlier
    case ChatRoomAssistantReconnect( chatRoomId, assistant, assistantActor ) if chatRoomId == chatRoom._id =>
      if( assistants.contains( assistant._id ) ) {
        assistants = assistants + (assistant._id -> ( assistant, Some(assistantActor) ) )
      }

      saveSystemChatMessage( models.ChatRooms.MessageType.AssistantReconnect, Json.obj( "assistantId" -> assistant._id ) )

    case ChatRoomAssistantLeave( chatRoomId, assistantId ) if chatRoomId == chatRoom._id.stringify  =>

      if( assistants.contains( assistantId ) ) {
        user.foreach { case (userActor, _) =>
          userActor ! UserAssistantChatRoomLeave(assistantId.stringify)
        }

        saveSystemChatMessage( models.ChatRooms.MessageType.AssistantLeave, Json.obj( "assistantId" -> assistantId ) )

        assistants = assistants - assistantId
      }

    case ChatRoomAssistantDisconnected( chatRoomId, assistantId ) if chatRoomId == chatRoom._id.stringify  =>
      assistants.get( assistantId ).foreach{ case ( assistant, _ ) =>

        user.foreach { case (userActor, _) =>
          userActor ! UserAssistantChatRoomLeave(assistantId.stringify)
        }

        assistants = assistants + ( assistantId -> ( assistant, None ) )

        saveSystemChatMessage( models.ChatRooms.MessageType.AssistantDisconnected, Json.obj( "assistantId" -> assistantId ) )
      }

    case ChatRoomAssistantMessage( chatRoomId, assistantId, message, created ) =>
      user.foreach{ case ( actor, _) =>
          actor ! UserChatMessageFromAssistant( message, assistantId.stringify, messageSequenceNum, created )
      }
      assistants.values.filter( _._1._id != assistantId ).foreach {
        case ( _, Some( actor ) ) =>
          actor ! AssistantChatMessageFromAssistant( chatRoomId, assistantId.stringify, message, messageSequenceNum, created )
        case _ =>
          // this assistant has quit
      }

      saveAssistantChatMessage( message, assistantId )

    case UserChatMessage( msg, created ) =>

      user.foreach{ case ( actor, userData ) =>

        assistants.foreach{
          case ( _, ( _, Some( assistantActor ) ) ) =>
            assistantActor ! AssistantChatMessageFromUser( chatRoom._id.stringify, userData._id.stringify, msg, messageSequenceNum, created )
          case _ =>
            // assistant ins offline
        }

        if( assistants.isEmpty ){
          companyActor ! AssistantChatMessageFromUser( chatRoom._id.stringify, userData._id.stringify, msg, messageSequenceNum, created )
        }

        saveUserChatMessage( msg, userData._id )

        // actor ! UserChatMessageFromSystem( "You wrote: " + msg )
      }

    case GetChatRoomData( replyTo ) =>
        replyTo ! getChatRoomData()

    case ChatRoomAssistantClose( chatRoomId, assistantId, reason ) =>
      user.foreach { case ( userActor, _ ) =>
        userActor ! UserChatRoomClosedByAssistant( assistantId.stringify, reason )
        userActor ! PoisonPill
      }

      assistants.values.foreach {
        case ( _, Some(actor) ) =>
          actor ! AssistantChatRoomClosedByAssistant( chatRoom._id.stringify, assistantId.stringify, reason )

        case _ =>
          // this assistant has quit already
      }

      closeChatRoom( None, Some(assistantId), Some(reason) )

      self ! PoisonPill


    case u@UserChatFinished( email, phone, comment, problemIsSolved ) =>

      user.foreach { case ( actor, _ ) =>
        actor ! UserChatRoomClosed()
        actor ! PoisonPill
      }

      assistants.foreach {
        case (_, (_, Some(actor ) )) =>
          actor ! AssistantChatRoomClosedByUser( chatRoom._id.stringify, email, phone, comment, problemIsSolved )
        case _ =>
          // assistant has quit
      }

      closeChatRoom( Some(ChatRoomUserFeedback( email, phone, comment, problemIsSolved )) )

      self ! PoisonPill


    case Terminated( child ) =>
      // println("terminated ", child)
      if( user.exists( _._1 == child ) ){

        // notify assistants
        user = None

        userDisconnectTimeout = Some( context.system.scheduler.scheduleOnce(userReconnectTimeout, self, UserDisconnectTimeout ) )
      }

    case ChatRoomSystemClose( reason ) =>

      assistants.foreach {
        case (_, (_, Some( actor ) )) =>
          actor ! AssistantChatRoomTerminated( chatRoom._id.stringify )

        case _ =>

      }

      closeChatRoom( None, None, Some(reason) )


    case WidgetDeleted =>

      user.foreach{ case (actor, _) =>
        actor ! UserChatRoomClosedBySystem( models.ChatRooms.CloseReason.WidgetDeleted )
        actor ! PoisonPill

      }

      assistants.foreach {
        case (_, (_, Some( actor ) )) =>


          actor ! AssistantChatRoomWidgetDeleted( chatRoom._id.stringify )
          actor ! AssistantChatRoomTerminated( chatRoom._id.stringify )

        case _ =>

      }

      closeChatRoom( None, None, Some( models.ChatRooms.CloseReason.WidgetDeleted ))

      self ! PoisonPill



    case UserDisconnectTimeout =>

      // Assistant Chat Room Terminated to all assistants

      companyActor ! ChatRoomUserDisconnectedTimeout( chatRoom._id )

      user.foreach{ case ( actor, user2 ) =>

        saveSystemChatMessage( models.ChatRooms.MessageType.UserDisconnected, Json.obj( "userId" -> user2._id ) )

        actor ! PoisonPill
      }

      closeChatRoom( None, None, Some( models.ChatRooms.CloseReason.UserDisconnected ))

      self ! PoisonPill

    case other =>
      Logger.debug("ChatRoom Actor - unknown message " + other)
  }

  def getChatRoomData() = {
    ChatRoomData(
      chatRoomCreator,
      user.isDefined,
      chatRoom,
      messages,
      assistants.values.filter( _._2.isDefined ).map( _._1._id.stringify)
    )
  }


  override def postStop() {
    super.postStop()
    Logger.debug("ChatRoom Actor - stop")
  }

  def saveUserChatMessage( message:String, userId: ObjId ) = {

    saveChatMessage( message, MessageFromTypes.user, userId )

  }

  def saveAssistantChatMessage( message:String, assistantId: ObjId ) = {

    saveChatMessage( message, MessageFromTypes.assistant, assistantId )

  }

  def saveSystemChatMessage( messageType:String, data:JsValue ) = {
    saveChatMessage(  data.toString(), MessageFromTypes.system, chatRoom.companyId, Some(messageType) )
  }

  def saveChatMessage( message:String, fromType: MessageFromTypes.MessageFromType, fromId: ObjId, messageType:Option[String] = None ) = {

    val msg = ChatRoomMessage( fromId, messageSequenceNum, fromType, message, messageType )

    messages = messages :+ msg

    messageSequenceNum += 1

    ChatRoom.saveChatRoomMessageinDB(fromType, chatRoom._id, msg)
  }

  def closeChatRoom( userFeedback:Option[ChatRoomUserFeedback] = None, assistantThatClosed: Option[ObjId] = None, assistantCloseReason:Option[String] = None ) = {

    user.foreach { case (actor, _) =>
      context.unwatch(actor)
    }

    models.ChatRooms.closeChatRoom( Json.obj("_id" -> chatRoom._id), userFeedback, assistantThatClosed, assistantCloseReason ).map { lastError =>
      if( lastError.ok ){

        Future.successful( true )

      } else {
        Future.failed(new Exception("Chat Room update failed"))
      }
    }
  }

}

object ChatRoom {
  import actors.messages.SocksMessages._
  import models.UserHelper._
  import models.ChatRooms.{ jsonFormat => j6 }
  import models.ChatRoomHelper._
  import models.Assistants.{ jsonFormat => j5 }
  import models.Companies.{ jsonFormat => j7 }
  import models.Widgets.{ jsonFormat => j8 }

  case object UserDisconnectTimeout
  case object WidgetDeleted

  case class GetChatRoomData( replyTo: ActorRef )

  case class ChatRoomData( chatRoomCreator: models.User, userConnected: Boolean, chatRoom:models.ChatRoom, messages:Iterable[ChatRoomMessage] = List(), connectedAssistants:Iterable[String] )

  case class UpdateChatRoomData( chatRoom:models.ChatRoom )

  case class ChatRoomAssistantJoin( chatRoomId:String, assistant: models.Assistant, assistantActor: ActorRef )  extends AssistantChatRoomRequest
  case class ChatRoomAssistantLeave( chatRoomId:String, assistantId:ObjId )  extends AssistantChatRoomRequest
  // a failure - assistant has quit without the "leaving" message.
  case class ChatRoomAssistantDisconnected( chatRoomId:String, assistantId:ObjId ) extends AssistantChatRoomRequest
  case class ChatRoomAssistantReconnect( chatRoomId:ObjId, assistant: models.Assistant, assistantActor: ActorRef)

  case class ChatRoomAssistantClose( chatRoomId:String, assistantId:ObjId, reason:String ) extends AssistantChatRoomRequest
  case class ChatRoomAssistantMessage( chatRoomId:String, assistantId:ObjId, message:String, created:Date ) extends AssistantChatRoomRequest

  case class ChatRoomUserDisconnectedTimeout( chatRoomId:ObjId )

  case class ChatRoomSystemClose( reason:String )

  protected val userReconnectAwaitTimeout = 5*60 // seconds

  implicit val chatRoomDataToJson = Json.format[ChatRoomData]

  def saveChatRoomMessageinDB(fromType: MessageFromType, chatRoomId: ObjId, msg: ChatRoomMessage) = {
    models.ChatRooms.collection.update(
      Json.obj("_id" -> chatRoomId),
      Json.obj(
        "$push" -> Json.obj(
          "messages" -> msg
        ),
        "$inc" -> Json.obj(
          "messageCount" ->
            (if ((fromType == MessageFromTypes.assistant) || (fromType == MessageFromTypes.user)) 1 else 0)
        ),
        "$set" -> Json.obj(
          "updated" -> new Date().getTime
        )
      )
    )
  }


}