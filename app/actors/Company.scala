package actors

import java.util.Date

import actors.ChatRoom._
import actors.Company.BusinessCatalystStartTokenRefresh
import actors.CompanyMaster.CompanyMessage
import actors.messages.SocksMessages._
import akka.actor._
import akka.pattern._
import akka.actor.Actor.Receive
import models._
import models.base.Collection._
import reactivemongo.bson.{BSONDocument, BSONObjectID}
import reactivemongo.play.json.BSONFormats._
import scala.concurrent.Future
import models.base.Collection.ObjId
import play.api.libs.concurrent.Execution.Implicits._
import akka.util.Timeout
import scala.concurrent.duration._
import scala.util._
import play.api.libs.json._
import play.api.Play.current
import akka.actor.Props
import scala.concurrent.duration.Duration
import reactivemongo.play.json._
import reactivemongo.play.json.BSONFormats.BSONObjectIDFormat
import play.modules.reactivemongo.json.collection._
import BusinessCatalystOAuth._

/**
 * User: aloise
 * Date: 18.10.14
 * Time: 23:17
 */
class Company( company:models.Company ) extends Actor {

  import Company._
  import models.ChatRooms.{ jsonFormat => chatRoomJsonFormat }
  import models.Assistants.{ jsonFormat => assistantJsonFormat }
  import models.ChatRoomHelper._

  // chatRoomActor, ChatRoom, current User ObjId
  var chatRooms:Map[ObjId, (ActorRef, models.ChatRoom )] = Map()

  var assistants: Map[ObjId,(ActorRef, models.Assistant)] = Map()

  var users: Map[ObjId, (ActorRef, models.User)] = Map()

  var oauthRefreshData:Option[ ExchangeTokenResponse ] = company.businessCatalystOAuthResponse
  var oauthRefreshInterval:Option[Cancellable] = None




  override def receive: Receive = {

    case n@NewUserJoin( widget, chatRoomId, userRequestData, userActor, visitorStats ) =>

      val me = self

      chatRoomId.fold[Future[Any]] {
        // chat room id is empty
        Company.createNewChatRoom( widget, userRequestData, visitorStats ) flatMap { chatRoomData =>
            val chatRoomActor = context.actorOf( chatRoomActorProps( chatRoomData ) )

            chatRoomJoinUser( me, n, chatRoomActor, chatRoomData, userActor)

        } recover {
          // error creating new chat room
          case ex:Throwable =>
            NewUserJoinFailed( n, ex )
        }

      } { chatRoomObjId =>

        chatRooms.get( chatRoomObjId ).fold[Future[Any]] {
          // chat Room not found
          // try to resurrect the chat room
          reloadChatRoom( chatRoomObjId ) flatMap { chatRoomData =>

            val chatRoomActor = context.actorOf(chatRoomActorProps(chatRoomData))

            chatRoomJoinUser(me, n, chatRoomActor, chatRoomData, userActor)

          } recover {
            case ex =>
              NewUserJoinFailed( n, new Exception("Chat Room Not Found") )
          }

        } { case ( chatRoomActor, chatRoomData ) =>

          chatRoomJoinUser( me, n, chatRoomActor, chatRoomData, userActor )

        }

      } pipeTo sender // sender

    case c@ChatRoomAssistantClose( chatRoomId, assistantId, reason ) =>
      BSONObjectID.parse( chatRoomId ) match {
        case Success( cId ) =>
          chatRooms.get( cId ).foreach { case ( chatRoomActor, _ ) =>
            chatRoomActor ! c
          }

        case _ =>

      }

    case ChatRoom.ChatRoomUserDisconnectedTimeout( chatRoomId ) =>
/*      chatRooms.get( chatRoomId ).foreach { case ( _, chatRoomData ) =>
        assistants.values.foreach { case ( assistantActor, _ ) =>
          assistantActor ! AssistantChatRoomTerminated( chatRoomId.stringify )
        }
      }*/

    case a@AssistantInfoUpdated( newAssistantInfo ) =>
      assistants.values.foreach { case ( actor, _ ) =>
        actor ! a
      }

      assistants.get( newAssistantInfo._id ).foreach{ case ( actor, _ ) =>
        assistants = assistants.updated(  newAssistantInfo._id, (actor, newAssistantInfo ) )
      }

      chatRooms.values.foreach{ case (actor, _) =>
        actor ! a
      }

    case a@AssistantCompanyInfoUpdated( newCompanyInfo ) =>
      assistants.values.foreach { case ( actor, _ ) =>
        actor ! a
      }



    case AssistantCompanyJoin( newAssistantActor, newAssistant, _ ) if newAssistant.companyId == company._id =>

        joinAssistant( newAssistant, newAssistantActor )

    case WidgetDeleted( widgetId ) =>
      chatRooms.values.foreach{ case ( actor, chatRoom ) =>
        if( chatRoom.widgetId == widgetId ){
          actor ! actors.ChatRoom.WidgetDeleted
        }

      }
      chatRooms = chatRooms.filter( _._2._2.widgetId != widgetId )


    case AssistantLogout( assistantIdStr ) =>
      BSONObjectID.parse( assistantIdStr ) match {
        case Success( assistantId ) =>

          assistants.get( assistantId ).foreach { case ( assistantActor, _ ) =>
            assistantActor ! PoisonPill

            assistantTerminated( assistantId )
          }

        case _ =>
      }


    case a:AssistantChatMessageFromUser =>
      // send it to all assistants

      assistants.values.foreach{ case ( actor, _ ) =>
          actor ! a
      }

    // it's called from the chatRoomJoinUser after the successful chat room join only
    case NewUserJoinedSuccessfully( NewUserJoin( widget, chatRoomId, userRequestData , userActor, visitorStats ),  chatRoomActor, ChatRoomData( _, _, chatRoomData, messages, activeAssistants ) ) =>

      // watch for chat room termination

      addChatRoom( chatRoomActor, chatRoomData )

      context.watch( userActor )

      users = users + ( chatRoomData.user._id -> ( userActor, chatRoomData.user ) )

      newUserJoinedSuccessfully( widget, chatRoomData, userRequestData, userActor, chatRoomData.user )

    case msg:ChatRoomExtendedInfo =>
      chatRooms.get( msg.chatRoomId ).foreach { case ( chatRoomActor, _ ) =>
        chatRoomActor ! UpdateChatRoomData( msg.chatRoomData )
        assistants.values.foreach { case ( assistantActor, _ ) =>
            chatRoomActor ! GetChatRoomData( assistantActor )
          }
      }

    case chatRoomLeaveMsg@ChatRoomAssistantLeave( chatRoomId, assistantId ) =>
      BSONObjectID.parse(chatRoomId) match {

        case Success( cId ) =>

          if( chatRooms.contains( cId ) ){

            chatRooms.get( cId ).foreach { case ( chatRoomActor , _ ) =>
              assistants.values.find( _._2._id == assistantId ).foreach{ _ =>

                chatRoomActor ! chatRoomLeaveMsg

                // notify all assistants
                assistants.values.filter( _._2._id != assistantId ).foreach{ case ( actor, _ ) =>
                  actor ! AssistantChatRoomAnotherAssistantLeaved( chatRoomId, assistantId.stringify )
                }
              }
            }

            // it's ok
          } else {
            // nothing
          }

        case Failure( ex ) =>
          // nothing
      }


    case c@ChatRoom.ChatRoomAssistantJoin( chatRoomId, assistant, assistantActor ) =>

      BSONObjectID.parse(chatRoomId) match {
        case Success( cId ) =>

          getChatRoomByIdOrRespawn( cId ).map { case ( chatRoomActor, chatRoomData ) =>
            chatRooms.get( cId ).foreach { case ( chatRoomActor , _ ) =>
              assistants.values.find( _._2._id == assistant._id ).foreach{ _ =>

                chatRoomActor ! c

                chatRoomActor ! ChatRoom.GetChatRoomData( sender() )

                // notify all assistants
                assistants.values.filter( _._2._id != assistant._id ).foreach{ case ( actor, _ ) =>
                  actor ! AssistantChatRoomAnotherAssistantJoined( chatRoomId, assistant._id.stringify )
                }
              }
            }

            AssistantChatRoomJoinSuccess( chatRoomId )
          } recover {
            case ex:Throwable =>
              AssistantChatRoomJoinFailure( chatRoomId, "chat_room_not_found" )
          } pipeTo sender()


        case Failure( ex ) =>
          sender ! AssistantChatRoomJoinFailure( chatRoomId, ex.toString )
      }

    case GetActiveAssistants =>
      sender ! GetActiveAssistantsResult(
        assistants.
          filter{ case (_,(_, assistant)) => assistant.status == models.Assistants.Status.Online }.
          map{ case (_,(_, assistant)) => assistant.copy( password = "" ) }.
          toSeq
      )

    // other chat room requests : messages etc
    case a:AssistantChatRoomRequest =>
      Try( BSONObjectID( a.chatRoomId ) ).foreach{ chatRoomObjId =>
        chatRooms.get( chatRoomObjId ).foreach{ case ( chatRoomActor, _ ) =>
          chatRoomActor.forward( a )
        }

      }

    case DeleteAssistant( assistantId ) =>
      assistants.get(assistantId).foreach{ case (assistantActor, _) =>

        assistantActor ! AssistantDeleted()
        assistantActor ! PoisonPill

        assistantTerminated( assistantId )
      }

    case CompanyDeleted( message ) =>
      assistants.values.foreach{ case ( assistantActor, _ ) =>
        assistantActor ! AssistantCompanyDeleted( message )
      }
      self ! PoisonPill

    case Terminated( actor ) =>

      chatRooms.find( _._2._1 == actor ).foreach { case ( _, ( _, c ) ) =>
        chatRooms = chatRooms - c._id

        // chat room terminated
        assistants.values.foreach { case ( assistantActor, _) =>
          assistantActor ! AssistantChatRoomTerminated( c._id.stringify )
        }


      }

      assistants.find( _._2._1 == actor ).foreach { case ( _, ( _, a ) ) =>
        // assistant quit
        assistantTerminated( a._id )
      }

      users.find( _._2._1 == actor ).foreach { case ( _, ( _, u ) ) =>

        // user disconnected
        chatRooms.values.find( _._2.user._id  == u._id ).foreach { chatRoom =>
          // notify assistants
          val chatRoomId = chatRoom._2._id.stringify

          assistants.values.foreach { case ( assistantActor, _ ) =>

            assistantActor ! AssistantChatRoomUserDisconnected( chatRoomId, u._id.stringify )
          }
        }

        users = users - u._id
      }

  }


  protected def newUserJoinedSuccessfully(widget: Widget, chatRoomData: models.ChatRoom, userRequestData: RequestUserData, userActor: ActorRef, user: User) = {

  }

  def reloadChatRoom(chatRoomObjId: ObjId):Future[models.ChatRoom] = {

    val query = Json.obj("_id" -> chatRoomObjId, "status" -> Json.obj("$ne" -> ChatRoomStatuses.closed) , "companyId" -> company._id )
    models.ChatRooms.collection.find(query).one[models.ChatRoom].map{
      case Some(chatRoom) =>
        chatRoom

      case _ =>
        throw new Exception("chat_room_not_found")
    }

  }

  def getChatRoomByIdOrRespawn( chatRoomId:ObjId ):Future[(ActorRef, models.ChatRoom)] = {

    chatRooms.get( chatRoomId ).map( Future.successful ).getOrElse {

      reloadChatRoom( chatRoomId ) map { chatRoomData =>

        val chatRoomActor = context.actorOf(chatRoomActorProps(chatRoomData))

        addChatRoom( chatRoomActor, chatRoomData )

        ( chatRoomActor, chatRoomData )
      } recover {
        case ex:Throwable =>
          throw new Exception("chat_room_not_found")
      }

    }

  }

  def addChatRoom( chatRoomActor:ActorRef, chatRoomData:models.ChatRoom ) = {

    context.watch( chatRoomActor )

    val chatRoomAlreadyExists = chatRooms.contains( chatRoomData._id )

    chatRooms = chatRooms + ( chatRoomData._id -> ( chatRoomActor, chatRoomData ) )

    // send a message to all assistants
    assistants.values.foreach { case ( assistantActor, _ ) =>
      // actor ! AssistantChatRoomCreated( chatRoomData, Some(user) )
      if( chatRoomAlreadyExists ) {
        // user connected message
        assistantActor ! AssistantChatRoomUserConnected( chatRoomData._id.stringify, chatRoomData.user )

      } else {
        // forward chat room data to the customer
        chatRoomActor ! ChatRoom.GetChatRoomData(assistantActor)
      }
    }

  }

  def assistantTerminated( assistantId: ObjId ) = {

    assistants.get(assistantId).foreach { case ( _, assistant ) =>
      assistants = assistants - assistantId

      assistants.values.foreach { case ( assistantActor, _) =>
        assistantActor ! AssistantAnotherAssistantQuitCompany( assistantId.stringify )
      }

      chatRooms.values.foreach{ case ( chatRoomActor, chatRoom ) =>
        chatRoomActor ! ChatRoom.ChatRoomAssistantDisconnected( chatRoom._id.stringify, assistantId )
      }

      models.Assistants.collection.update(Json.obj("_id" -> assistant._id ), Json.obj(
        "$set" -> Json.obj(
          "status" -> models.Assistants.Status.Away,
          "statusBeforeDisconnect" -> assistant.status
        ))
      )

    }
  }

  def getAssistantCompanyInfoMessage(): Future[AssistantCompanyInfo] = {
    models.Assistants.collection.find( Json.obj( "companyId" -> company._id ) ).cursor[models.Assistant]().collect[List]().map{ assistants =>
      AssistantCompanyInfo( company, assistants.map( _.copy( password = "" ) ) )
    }
  }

  def joinAssistant(newAssistant: Assistant, newAssistantActor: ActorRef): Unit = {

    // disconnect the old socket
    assistants.get( newAssistant._id ).foreach { case ( actor , _ ) =>
      actor ! AssistantWebsocketReconnectClose()
    }
    // remove the existing assistant data
    assistants = assistants - newAssistant._id

    context.watch( newAssistantActor )

//    getAssistantCompanyInfoMessage( ) pipeTo newAssistantActor

    // sender -> new assistant actor
    newAssistantActor ! AssistantCompanyJoinSuccess( self )

    // send current chat rooms
    chatRooms.values.foreach{ case ( chatRoomActor, chatRoomData ) =>

      // let's try to reconnect it where possible
      chatRoomActor ! ChatRoom.ChatRoomAssistantReconnect( chatRoomData._id, newAssistant, newAssistantActor )

      // sender ! AssistantChatRoomCreated( chatRoomData, users.get( chatRoomData.userId ).map( _._2 ) )
      chatRoomActor ! ChatRoom.GetChatRoomData( newAssistantActor )

    }

    // send assistants
    assistants.values.foreach{ case ( assistantActor, assistantData ) =>
      // notify new user
      sender ! AssistantAnotherAssistantJoinedCompany( assistantData.copy( password = "") )
      // notify existing assistant
      assistantActor ! AssistantAnotherAssistantJoinedCompany( newAssistant )
    }

    // update the list
    assistants = assistants + ( newAssistant._id -> ( newAssistantActor, newAssistant ) )

  }

  protected def chatRoomJoinUser( companyActor:ActorRef, joinRequest: NewUserJoin, chatRoomActor:ActorRef, chatRoomData:models.ChatRoom, userActor:ActorRef )( implicit timeout:Timeout = Timeout(60.seconds) ): Future[Any] = {

    (chatRoomActor ask UserChatRoomJoin(userActor, chatRoomData.user)) map {
      case UserChatRoomJoinedSuccessfully(_, updatedChatRoomData ) =>

        val msg = NewUserJoinedSuccessfully(joinRequest, chatRoomActor, updatedChatRoomData )
        // notify myself about successful room join
        companyActor ! msg
        msg
      case _ =>
        NewUserJoinFailed(joinRequest, new Exception("User Char Room Join failed"))
    } recover {
      case ex: Throwable =>
        NewUserJoinFailed(joinRequest, ex)
    }
  }

  protected def chatRoomActorProps( c: models.ChatRoom ) =
    Props( classOf[actors.ChatRoom], c, self )

  override def postStop(): Unit = {

    assistants.foreach { case ( _, (a, _)) =>
      a ! AssistantCompanyStop( models.ChatRooms.CloseReason.SystemRestart )
      a ! PoisonPill
    }

    chatRooms.foreach { case ( _, (a, _)) =>
      a ! ChatRoomSystemClose( models.ChatRooms.CloseReason.SystemRestart )
      a ! PoisonPill
    }
    users.foreach { case ( _, (a, _)) =>
      a ! PoisonPill
    }

  }


}

object Company {

  import models.ChatRooms.{ jsonFormat => chatRoomJsonFormat }
  import models.Assistants.{ jsonFormat => assistantJsonFormat }
  import models.Companies.{ jsonFormat => companyJsonFormat }


  class CompanyJoinException(msg:String) extends Exception(msg)

  case class NewUserJoin( widget: models.Widget, chatRoomId:Option[ObjId], userRequestData: RequestUserData, userActor:ActorRef, visitorStats:VisitorStats )


  case class ChatRoomExtendedInfo( chatRoomId:ObjId , chatRoomData:models.ChatRoom)

  case class NewUserJoinedSuccessfully( joinRequest:NewUserJoin, chatRoomActor:ActorRef, chatRoomData:ChatRoomData )
  case class UserChatRoomDisconnected( chatRoom: models.ChatRoom, user:models.User )
  case class UserChatRoomReconnected( chatRoom: models.ChatRoom, user:models.User )
  case class NewUserJoinFailed(  joinRequest:NewUserJoin, ex:Throwable )

  object GetActiveAssistants
  case class GetActiveAssistantsResult( assistants:Seq[models.Assistant])
  case class DeleteAssistant( assistantId:ObjId )

  case class WidgetDeleted( widgetId: ObjId )

  case class CompanyDeleted( message:String = "" )


  case class BusinessCatalystStartTokenRefresh( data:ExchangeTokenResponse )
  case object BusinessCatalystTokenRefreshTick

  case class BusinessCatalystTokenRefreshResult( result:Try[ExchangeTokenResponse])

  case object BusinessCatalystStopTokenRefresh


  def refreshOAuthTokens() = {

    val q = Json.obj( "businessCatalystOAuthResponse.refresh_token" -> Json.obj( "$ne" -> JsNull ) )

    models.Companies.collection.find(q).cursor[models.Company]().collect[Seq]().foreach { companies =>

      companies.foreach { company =>
        global.Application.companyMaster ! CompanyMessage( company._id, BusinessCatalystTokenRefreshTick )
      }

    }

  }



  def createNewChatRoom( widget: models.Widget, userRequestData:RequestUserData, visitorStats: VisitorStats, isOffline:Boolean = false ) :Future[models.ChatRoom] = {

    val user = models.User(
      BSONObjectID.generate,
      userRequestData.name,
      userRequestData.email,
      userRequestData.company,
      widget._id,
      visitorStats,
      Some( UserHelper.getRandomColor ),
      widget.companyId
    )

    val chatRoom = models.ChatRoom(
      BSONObjectID.generate,
      user,
      widget._id,
      widget.companyId,
      userRequestData.page,
      if( isOffline ) Some(new Date()) else None,
      status = if( isOffline ) ChatRoomStatuses.closed else ChatRoomStatuses.pending,
      isOfflineMessage = Some( isOffline )
    )

    models.ChatRooms.collection.insert( chatRoom ).map { lastError =>
      if( lastError.ok ){

        chatRoom


      } else {
        throw new Exception("Chat Room save failed")
      }
    }

  }


}