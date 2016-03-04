package actors

import actors.AssistantConnection.AssistantLoginException
import actors.ChatRoom.{ChatRoomData, ChatRoomAssistantClose, ChatRoomAssistantLeave, ChatRoomAssistantJoin}
import actors.Company.NewUserJoin
import actors.messages.SocksMessages._
import akka.actor.{PoisonPill, Actor, ActorRef, Props}
import controllers.helpers.AssistantAuthAction
import models.ChatRoomMessage
import play.api.mvc.RequestHeader
import play.sockjs.api.SockJS
import play.sockjs.api.SockJS.MessageFormatter
import play.api.libs.json._
import akka.pattern._
import play.api.libs.concurrent.Execution.Implicits._
import reactivemongo.api.QueryOpts
import reactivemongo.play.json.BSONFormats._
import actors.messages.SocksMessages
import play.api.Logger
import reactivemongo.play.json._
import reactivemongo.play.json._
import reactivemongo.play.json.BSONFormats.BSONObjectIDFormat
import play.modules.reactivemongo.json.collection._
/**
 * User: aloise
 * Date: 02.11.14
 * Time: 10:17
 */
class AssistantConnection( request:RequestHeader, assistantSocksActor:ActorRef ) extends Actor {

  import UserConnection._
  import models.Widgets.{ jsonFormat => wigetsJsonFormat }
  import models.Assistants.{ jsonFormat => assistantsJsonFormat }
  import models.Companies.{ jsonFormat => companyJsonFormat }
  import models.ChatRooms.{ jsonFormat => chatRoomJsonFormat }

  val sessionId = ""

  def receive:Receive = receiveAuthorization

  def receiveAuthorization:Receive = pingReceive orElse {
    case AssistantLoginSuccess( assistant, company ) =>

      // company join
      val joinMsg = CompanyMaster.CompanyMessage( assistant.companyId, AssistantCompanyJoin( self, assistant, company ) )

      global.Application.companyMaster ! joinMsg

      context.become( receiveJoinCompany( assistant, company ) )

    case AssistantLoginFailure( ex ) =>
      assistantSocksActor ! Error( ex.getClass.getName, ex.getMessage )
      self ! PoisonPill

  }

  def receiveJoinCompany(assistant:models.Assistant, company: models.Company):Receive = pingReceive orElse {

    case AssistantCompanyJoinSuccess( companyActor ) =>

      assistantSocksActor ! AssistantGreeting( global.Application.appName , global.Application.appVersion )

      context.become( receiveNormal( assistant, company, companyActor ) )

    case AssistantCompanyJoinFailure( ex ) =>
      assistantSocksActor ! Error( ex.getClass.getName, ex.getMessage )
      self ! PoisonPill
  }

  def receiveNormal(assistant:models.Assistant, company: models.Company, companyActor:ActorRef):Receive = pingReceive orElse {

    case ChatRoomData( chatRoomCreator, userConnected, chatRoom, messages, connectedAssistants ) =>
      // collect visitor stats

      (
        for {
          chatUserStats <- models.Pageviews.getChatRoomUserStats( chatRoomCreator.stats.trackingCookie, 5 )
          userInfo <- models.UserInfos.findByTrackingCookie( company._id, chatRoomCreator.stats.trackingCookie )
        } yield AssistantChatRoomCreated( chatRoomCreator, userConnected, chatUserStats, userInfo, chatRoom, messages, connectedAssistants )

      ) pipeTo assistantSocksActor



    case a@AssistantWebsocketReconnectClose( t ) =>
      assistantSocksActor ! a
      self ! PoisonPill

    case AssistantChatMessage( message, chatRoomId, created ) =>
      companyActor ! ChatRoom.ChatRoomAssistantMessage( chatRoomId, assistant._id, message, created )

    case AssistantChatRoomClose( reason:String, chatRoomId:String ) =>
      companyActor ! ChatRoomAssistantClose( chatRoomId, assistant._id, reason )

    case AssistantChatRoomJoin( chatRoomId ) =>
      companyActor ! ChatRoomAssistantJoin( chatRoomId, assistant.copy( password = "" ) , self )

    case AssistantChatRoomLeave( chatRoomId ) =>
      companyActor ! ChatRoomAssistantLeave( chatRoomId, assistant._id )

    case AssistantUpdateUserInfo( cookie, info ) =>
      models.UserInfos.updateUserInfo( company._id, cookie, info )

    case AssistantGetUserPastChats( trackingCookie, page, itemsPerPage ) =>
      val chatRoomsF =
        models.ChatRooms.collection.
          find( Json.obj( "user.stats.trackingCookie" -> trackingCookie, "isDeleted" -> false ) ).
          sort( Json.obj( "created" -> -1 ) ).
          options( QueryOpts( itemsPerPage*page, itemsPerPage) ).
          cursor[models.ChatRoom]().
          collect[Seq](itemsPerPage)

      chatRoomsF map { list =>
        AssistantGetUserPastChatsResult( trackingCookie, page, itemsPerPage, list )
      } recover {
        case ex =>
          Logger.debug("AssistantGetUserPastChatsResult - ex " + ex)
          AssistantGetUserPastChatsResult( trackingCookie, page, itemsPerPage, Seq() )
      } pipeTo sender


    // other request
    case a:AssistantChatRoomRequest =>
      // pass it to the company
      companyActor ! a

    // responses
    case wsResponse:Response =>
      assistantSocksActor ! wsResponse
  }

  def pingReceive:Receive = {
    case AssistantPing( _ ) =>
      assistantSocksActor ! Pong()
  }

  override def preStart( ) = {
    // try to login with the session
    AssistantAuthAction.Implicits.getObject( request ) flatMap { assistant =>
        val companyF = models.Companies.collection.find( Json.obj( "_id" -> assistant.companyId ) ).one[models.Company]

        companyF.map {
          case Some( company ) =>
            AssistantLoginSuccess( assistant, company)
          case None =>
            throw new Exception("company_not_found")
        }


    } recover {
      case ex =>
        AssistantLoginFailure( new AssistantLoginException( "login_failure" ) )
    } pipeTo self

  }

}


object AssistantConnection {

  class AssistantLoginException( str:String ) extends Exception(str)

  implicit val assistantRequestMessageFormatter: MessageFormatter[AssistantRequest] = customMessageFormatter[AssistantRequest]

  def getActorProps( request:RequestHeader ):SockJS.HandlerProps = {
    assistantSocksActor => Props( classOf[AssistantConnection], request, assistantSocksActor )
  }

}