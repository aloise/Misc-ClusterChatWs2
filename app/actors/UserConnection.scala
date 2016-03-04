package actors

import java.net.URLDecoder
import java.util.Date
import models.{UserHelper, UserStatsOSData, UserStatsUserAgent, VisitorStats}
import play.api.mvc.RequestHeader
import play.sockjs.api.SockJS.MessageFormatter

import scala.concurrent.Future
import reactivemongo.play.json.BSONFormats._
import actors.Company.NewUserJoin
import actors.messages.SocksMessages._
import akka.util.Timeout
import models.base.Collection._
import play.api.Logger
import play.api.Play.current
import akka.actor._
import play.sockjs.api.SockJS
import play.api.libs.json._
import akka.pattern._
import akka.util.Timeout
import reactivemongo.bson.BSONObjectID
import scala.concurrent.duration._
import play.api.libs.concurrent.Execution.Implicits._
import akka.pattern._
import UserConnection._
import models.Widgets.{ jsonFormat => wigetsJsonFormat }
import models.UserHelper.userJsonFormat
import scala.util.Try
import net.sf.uadetector.service.UADetectorServiceFactory
import net.sf.uadetector.UserAgent
import net.sf.uadetector.UserAgentStringParser
import com.netaporter.uri.dsl._
import com.netaporter.uri._
import reactivemongo.play.json._
import reactivemongo.play.json.BSONFormats.BSONObjectIDFormat
import play.modules.reactivemongo.json.collection._

/**
 * User: aloise
 * Date: 18.10.14
 * Time: 22:26
 */

class UserConnection( userSocksActor:ActorRef, request:RequestHeader ) extends Actor {

  // wait for an disconnect
  context.watch( userSocksActor )

  def receive:Receive = receiveOnInitialization

  def receiveOnInitialization:Receive = pingReceive orElse socketTermination orElse {

    case UserConnectRequest( widgetId, chatRoomId, user ) =>
      initData( widgetId, chatRoomId, user )

    case UserConnection.InitializationFailed( ex ) =>
      errorAndStop(ex)

    case UserConnection.InitializedSuccessfully( widget, initialChatIdOpt, requestUserData, visitorData, assistants ) =>
        userSocksActor ! UserGreeting( global.Application.appName, global.Application.appVersion, widget.userGreetingMessage, assistants )

        joinChatRoom( widget, initialChatIdOpt, requestUserData, visitorData )
        context.become( receiveOnChatRoomJoin( widget, initialChatIdOpt ) )

    case x =>
      Logger.debug("actors.UserConnection::receiveOnInitialization - got a message during initialization " + x)

  }

  def receiveOnChatRoomJoin( widget:models.Widget, initialChatIdOpt:Option[ObjId] ):Receive = pingReceive orElse {

    case Company.NewUserJoinedSuccessfully( _, chatRoomActor, chatRoomData ) =>

//      Logger.debug( "actors.UserConnection::receiveOnChatRoomJoin - NewUserJoinedSuccessfully")

      userSocksActor ! UserConnected( chatRoomData.chatRoom._id.stringify , chatRoomData )

      context.become( receiveInChatroom( chatRoomActor, chatRoomData.chatRoom, widget, initialChatIdOpt ) )

    case Company.NewUserJoinFailed( _, ex ) =>
      errorAndStop(ex)

    case x =>
      Logger.debug("actors.UserConnection::receiveOnChatRoomJoin - got a message during initialization " + x)

  }

  def receiveInChatroom( chatRoomActor:ActorRef, chatRoom:models.ChatRoom, widget:models.Widget, initialChatIdOpt:Option[ObjId] ):Receive = pingReceive orElse {
    // outbound data
    case j: Response with Message  =>
      userSocksActor ! j

    // inbound data
    case u:UserRequest =>
      chatRoomActor ! u

    case unknown =>
      Logger.debug( "actors.UserConnection::receiveInChatroom - Unknown message " + unknown )

  }

  def pingReceive:Receive = {
    case Ping( _ ) =>
      userSocksActor ! Pong()
  }

  def socketTermination:Receive = {
    case Terminated(socketActor) if socketActor == userSocksActor =>
      self ! PoisonPill
      // we've got a disconnect
  }


  def joinChatRoom( widget: models.Widget, initialChatIdOpt:Option[ObjId], userRequestData: RequestUserData, visitorStats:VisitorStats ) = {

    //    implicit val t = Timeout( 30.seconds )

    val joinMsg = CompanyMaster.CompanyMessage( widget.companyId, NewUserJoin( widget, initialChatIdOpt, userRequestData , self, visitorStats ) )

    global.Application.companyMaster ! joinMsg
  }



  def initData( widgetId:String, chatRoomIdOpt:Option[String], userData:RequestUserData ) = {
    // notify and connect to the company
    val widgetIdObjOpt = Try( BSONObjectID(widgetId) ).toOption

    widgetIdObjOpt.fold[Future[Any]]{
      Future.successful( InitializationFailed( new Exception("widget_id_is_wrong") ) )
    } { widgetIdObj =>

      val chatRoomIdObj =
        chatRoomIdOpt.
          flatMap( s => if ( s.trim.isEmpty ) None else Some(s) ).
          flatMap( c => Try( BSONObjectID(c) ).toOption )

      models.Widgets.collection.find(Json.obj("_id" -> widgetIdObj )).one[models.Widget].flatMap {
        case Some(widget) if !widget.isDeleted =>

            models.Assistants.companyAssistantsInfo( widget.companyId ).map { assistants =>
              val visitorStats = models.Pageviews.getVisitorStats( request, userData.trackingCookie, Some( userData.page ) )

              InitializedSuccessfully( widget, chatRoomIdObj, userData, visitorStats, assistants )

            }

        case Some(widget) if widget.isDeleted =>
          Future.successful( InitializationFailed( new Exception( models.ChatRooms.CloseReason.WidgetDeleted ) ) )

        case None =>
          Future.successful( InitializationFailed( new Exception("WidgetNotFound") ) )
      }

    } pipeTo self

  }

  def errorAndStop(ex:Throwable) = {
    userSocksActor ! Error( ex.getClass.getName, ex.getMessage )
    userSocksActor ! PoisonPill
    self ! PoisonPill

  }

  override def postStop( ) = {
    context.unwatch( userSocksActor )
    // userSocksActor ! PoisonPill
  }


}

object UserConnection {

  import actors.messages.SocksMessages

  implicit val userRequestMessageFormatter: MessageFormatter[UserRequest] = customMessageFormatter[UserRequest]


  case class InitializationFailed( error : Exception )
  case class InitializedSuccessfully( widget:models.Widget, initialChatRoomIdOpt:Option[ObjId], userRequestData: RequestUserData, visitorStats:VisitorStats, assistants:Seq[models.AssistantInfo] )

  def getActorProps( request:RequestHeader ):SockJS.HandlerProps = {
    userSocksActor => Props( classOf[UserConnection], userSocksActor, request )
  }


}

