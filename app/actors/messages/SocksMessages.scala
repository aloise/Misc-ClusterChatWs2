package actors.messages

import java.rmi.server.ObjID
import java.util.Date

import actors.ChatRoom.ChatRoomData
import akka.actor.ActorRef
import julienrf.variants.Variants
import models.base.Collection.ObjId
import models._
import play.api.libs.json._

import scala.util.Try


/**
 * Created by Aloise on 16.10.14.
 */

object SocksMessages {

  import play.sockjs.api.SockJS._
  import ChatRoomHelper._
  import scala.reflect.ClassTag
  import models.Assistants.{ jsonFormat => j0 }
  import models.Companies.{ jsonFormat => j1 }
  import models.UserHelper.{ userJsonFormat => j2 }
  import models.ChatRooms.{ jsonFormat => j3 }
  import models.Pageviews.{ jsonFormat => j4 }
  import models.UserInfos.{ jsonFormat => j5 }
  import models.Assistants.{ assistantInfoJsonFormat => j6 }

  import models.Pageviews.chatRoomUserStatsToJson
  import models.ChatRoomUserStats
  import actors.ChatRoom.chatRoomDataToJson

  class UserRequestParseException(message:String) extends Exception(message)
  class MessageParseException(message:String) extends Exception(message)



  sealed trait Message
  sealed trait InternalMessage

  trait Request
  trait Response

  trait UserRequest extends Request
  trait UserResponse extends Response

  trait AssistantRequest extends Request
  trait AssistantResponse extends Response



  trait AssistantChatRoomRequest extends AssistantRequest {
    def chatRoomId:String
  }


  case class RequestUserData( name:String, email:String, company:String, trackingCookie: Option[String], page: String )

  // user requests

  case class UserConnectRequest(  widgetId:String, chatRoomId:Option[String], user: RequestUserData ) extends Message with UserRequest

  case class UserChatMessage( message:String, created: Date ) extends Message with UserRequest
  // case class UserConnect( chatRoomId:String, widgetId:String ) extends Message with UserRequest
  case class UserChatFinished( email:String, phone:String, comment:String, problemIsSolved:Boolean ) extends Message with UserRequest
  case class UserChatRoomJoin( userSocksActor:ActorRef, user:models.User ) extends InternalMessage

  // user responses
  case class UserGreeting( status:String, version:String, chatUserGreetingMessage:String, assistants:Iterable[models.AssistantInfo] ) extends Message with UserResponse

  case class UserConnected( chatRoomId:String, chatRoomData:ChatRoomData ) extends Message with UserResponse
  case class UserConnectFailed(error:String ) extends Message with UserResponse

  case class UserChatRoomClosedByAssistant( assistantId:String, reason:String, created:Date = new Date() ) extends Message with UserResponse
  case class UserChatRoomClosedBySystem( reason:String, created:Date = new Date() ) extends Message with UserResponse
  case class UserChatRoomClosed( created:Date = new Date() ) extends Message with UserResponse

  case class UserChatRoomJoinedSuccessfully( chatRoomActor:ActorRef, chatRoomData: ChatRoomData ) extends InternalMessage

  case class UserAssistantChatRoomJoin( assistantId: String, name:String, avatar:Option[String] ) extends Message with UserResponse

  case class UserAssistantChatRoomLeave( assistantId:String ) extends Message with UserResponse

  case class UserChatMessageFromAssistant( message:String, assistantId:String, messageSequenceNum:Int, created:Date = new Date() ) extends Message with UserResponse // might be send from Assistant and received by User
  case class UserChatMessageFromSystem( message:String, messageSequenceNum:Int, created:Date = new Date() ) extends Message with UserResponse // might be send from Assistant and received by User



  // assistant chat room requests
  case class AssistantChatMessage( message:String, chatRoomId: String, created:Date = new Date()) extends Message with AssistantChatRoomRequest
  case class AssistantCompanyJoin( assistantActor: ActorRef, assistant: models.Assistant, company: models.Company )  extends InternalMessage
  case class AssistantChatRoomClose( reason:String, chatRoomId:String ) extends Message with AssistantChatRoomRequest
  case class AssistantChatRoomJoin( chatRoomId:String ) extends Message with AssistantRequest
  case class AssistantChatRoomLeave( chatRoomId:String ) extends Message with AssistantRequest


  case class AssistantUpdateUserInfo( trackingCookie:String, userInfo:UserInfo ) extends Message with AssistantRequest
  case class AssistantGetUserPastChats( trackingCookie:String, page:Int, itemsPerPage:Int ) extends Message with AssistantRequest

  case class GetAssistantInfo( assistantId:String ) extends Message with AssistantRequest

  case class AssistantLogout( assistantId:String ) extends Message with AssistantRequest

  // assistant responses
  case class AssistantLoginSuccess( assistant: models.Assistant, company: models.Company ) extends InternalMessage with AssistantResponse
  case class AssistantLoginFailure( ex:Throwable ) extends InternalMessage with AssistantResponse
  // websocket is closed due to reconnect in different browser window
  case class AssistantWebsocketReconnectClose( created:Date = new Date() ) extends Message with AssistantResponse
  case class AssistantCompanyInfo( company:models.Company, assistants:Seq[models.Assistant] ) extends Message with AssistantResponse
  case class AssistantGreeting( status:String, version:String ) extends Message with AssistantResponse
  case class AssistantDeleted( message:String = "" ) extends Message with AssistantResponse
  case class AssistantCompanyDeleted( message:String = "" ) extends Message with AssistantResponse

  // assistant response - join chat room
  case class AssistantChatRoomJoinSuccess( chatRoomId:String )  extends Message with AssistantResponse
  case class AssistantChatRoomJoinFailure( chatRoomId:String, reason:String )  extends Message with AssistantResponse

  // assistants joins company - verification
  case class AssistantCompanyJoinSuccess( companyActor:ActorRef )
  case class AssistantCompanyJoinFailure( ex:Throwable )


  // assistant - another assistant actions
  case class AssistantAnotherAssistantJoinedCompany( assistant: models.Assistant )  extends Message with AssistantResponse
  case class AssistantAnotherAssistantQuitCompany( assistantId: String )  extends Message with AssistantResponse
  case class AssistantInfoUpdated( assistant: models.Assistant )  extends Message with Response
  case class AssistantCompanyInfoUpdated( company: models.Company )  extends Message with Response


  // assistant chat responses
  case class AssistantChatMessageFromUser( chatRoomId:String, userId:String, message:String, messageSequenceNum:Int, created: Date = new Date() ) extends Message with AssistantResponse
  case class AssistantChatMessageFromAssistant( chatRoomId:String, assistantId:String, message:String, messageSequenceNum:Int, created: Date = new Date() ) extends Message with AssistantResponse
  case class AssistantChatMessageFromSystem( chatRoomId:String, message:String, messageSequenceNum:Int, created: Date = new Date() ) extends Message with AssistantResponse

  case class AssistantChatRoomUserDisconnected( chatRoomId:String, userId: String ) extends Message with AssistantResponse
  case class AssistantChatRoomUserConnected( chatRoomId:String, user: models.User ) extends Message with AssistantResponse

  case class AssistantGetUserPastChatsResult( trackingCookie:String, page:Int, itemsPerPage:Int, chatRooms: Seq[models.ChatRoom] = Nil) extends Message with AssistantResponse


  // assistant chat room responses
  case class AssistantChatRoomCreated( chatRoomCreator: models.User, userConnected: Boolean, userStats: ChatRoomUserStats, userInfo:Option[UserInfo], chatRoom:models.ChatRoom, messages:Iterable[ChatRoomMessage] = List(), connectedAssistants:Iterable[String], created: Date = new Date() ) extends Message with AssistantResponse
//  case class AssistantChatRoomExtendedInfo( chatRoomId: String, bcCrmCustomerInfo: Option[BcCrmCustomerInfo] ) extends Message with AssistantResponse
  case class AssistantChatRoomUpdated( chatRoom:models.ChatRoom ) extends Message with AssistantResponse
  case class AssistantChatRoomClosedByUser( chatRoomId:String,  email:String, phone:String, comment:String, problemIsSolved:Boolean ) extends Message with AssistantResponse
  case class AssistantChatRoomClosedByAssistant( chatRoomId:String, assistantId:String , reason:String ) extends Message with AssistantResponse
  case class AssistantChatRoomTerminated( chatRoomId:String )  extends Message with AssistantResponse
  case class AssistantChatRoomWidgetDeleted( chatRoomId:String )  extends Message with AssistantResponse
  case class AssistantChatRoomAnotherAssistantJoined( chatRoomId:String, assistantId: String ) extends Message with AssistantResponse
  case class AssistantChatRoomAnotherAssistantLeaved( chatRoomId:String, assistantId: String ) extends Message with AssistantResponse

  case class AssistantCompanyStop( reason:String )  extends Message with AssistantResponse

  // system messages
  // case class SystemChatMessage( message:String, created:Date = new Date() ) extends Message with Response
  case class Error( name: String, description: String = "") extends Message with Response
  case class Ping( created:Date = new Date() ) extends Message with UserRequest
  case class AssistantPing( created:Date = new Date() ) extends Message with AssistantRequest
  case class Pong( created:Date = new Date() ) extends Message with Response

  implicit val messageFormat: Format[Message] = Variants.format[Message]((__ \ "event").format[String])
  implicit val requestUserDataJsonFormatter = Json.format[RequestUserData]
  implicit val requestConnectionData = Json.format[UserConnectRequest]

  implicit val messageFormatter: MessageFormatter[Message] =
    MessageFormatter(
      data => {
        val js = try {
          Json.parse( data )
        } catch {
          case e:Throwable =>
            throw new MessageParseException( e.getMessage )
        }

        messageFormat.reads( js ) match {
          case JsSuccess(obj, _) =>
            obj
          case JsError(_) =>
            throw new MessageParseException("Message Parse Error")
        }

      } , {
        case m:Message =>
          Json.stringify( messageFormat.writes(m) )
        case _ =>
          throw new MessageParseException( "Invalid Message Class" )
      }
    )

  def customMessageFormatter[T : ClassTag ]:MessageFormatter[T] =
    MessageFormatter(
        data => {

          val js = try {
            Json.parse( data )
          } catch {
            case e:Throwable =>
              throw new MessageParseException( e.getMessage )
          }

          val clazz = implicitly[ClassTag[T]].runtimeClass

          messageFormat.reads( js ) match {
            case JsSuccess(obj, _) =>
              obj match {
                case req:T if clazz.isInstance(req) =>
                  req
                case _ =>
                  throw new MessageParseException( "Invalid Message Class" )
              }
            case JsError(_) =>
              throw new MessageParseException( "Message Parse Error" )
          }

        },
        {
          case r:Message =>
            Json.stringify(messageFormat.writes(r))
          case _ =>
            throw new MessageParseException( "Invalid Message Class" )
        }
    )




}
