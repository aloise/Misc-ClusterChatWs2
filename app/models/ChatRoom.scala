package models

import java.util.Date

import models.UserInfos._
import models.base.Collection
import models.base.Collection._
import play.api.libs.json.{JsObject, JsValue, Json}
import reactivemongo.bson.BSONObjectID
import play.modules.reactivemongo.json.BSONFormats._
import reactivemongo.api.indexes.{IndexType, Index}
import models.UserHelper.userJsonFormat
import play.api.libs.concurrent.Execution.Implicits._
import scala.concurrent.Future
import scala.util.Try
import play.modules.reactivemongo.json._
import play.modules.reactivemongo.json.BSONFormats.BSONObjectIDFormat
import play.modules.reactivemongo.json.collection._
import BusinessCatalystOAuth._


/**
 * Created by pc3 on 16.10.14.
 */

case class ChatRoomMessage (
   from: ObjId, // User._id, Assistant._id, Company._id
   sequenceNum:Int,
   fromType: MessageFromTypes.MessageFromType, // from MessageFromTypes
   message:String, // contains Json for specific message types
   messageType: Option[String] = None,
   created:Date = new Date()
)

case class ChatRoomUserFeedback( email:String, phone:String, comment:String, problemIsSolved:Boolean)

case class ChatRoom (
  _id:ObjId,
  user: models.User,
  widgetId:ObjId,
  companyId: ObjId,
  page: String,
  closed:Option[Date] = None,
  messages:Seq[ChatRoomMessage] = Seq(),
  status:ChatRoomStatuses.Status = ChatRoomStatuses.pending,
  userFeedback: Option[ChatRoomUserFeedback] = None,
  bcCrmCustomerInfo:Option[BcCrmCustomerInfo] = None,
  assistantCloseReason: Option[String] = None,
  closedByAssistantId:Option[ObjId] = None, // it's populated from : ChatRooms.AssistantCloseReason, ChatRooms.CloseReason
  messageCount:Int = 0,
  isDeleted:Boolean = false,
  isOfflineMessage:Option[Boolean] = None,
  updated:Option[Date] = None,
  created:Date = new Date()
)


object MessageFromTypes {

  type MessageFromType = Int

  val user:MessageFromType = 0
  val assistant:MessageFromType = 1
  val system:MessageFromType = 2
}

object ChatRoomStatuses {

  type Status = Int

  val pending:Status = 0 // no assistants
  val open:Status = 1 // chat is in progress
  val closed:Status = 2 // chat was closed
}

object ChatRoomHelper {


  implicit val chatRoomMessageJsonFormat = utils.Json.toOFormat( Json.format[ChatRoomMessage] )
  implicit val chatRoomUserFeedbackJsonFormat = utils.Json.toOFormat( Json.format[ChatRoomUserFeedback] )
  // implicit val bcCrmCustomerInfo = utils.Json.toOFormat( Json.format[BcCrmCustomerInfo] )

}

import ChatRoomHelper._
import models.BusinessCatalystOAuth._
import models.Companies.{ jsonFormat =>j1 }


object ChatRooms extends Collection("chat_rooms", Json.format[ChatRoom]) {

  object MessageType {
    val UserJoin = "UserJoin"
    val UserLeave = "UserLeave"
    val UserDisconnected = "UserDisconnected"
    val AssistantJoin = "AssistantJoin"
    val AssistantLeave = "AssistantLeave"
    val AssistantDisconnected = "AssistantDisconnected"
    val AssistantReconnect = "AssistantReconnect"
  }

  object AssistantCloseReason {
    val Resolved = "Resolved"
    val Spam = "Spam"
    val NotResponding = "NotResponding"
  }

  object CloseReason {
    val UserDisconnected = "UserDisconnected"
    val SystemRestart = "SystemRestart"
    val WidgetDeleted = "WidgetDeleted"
  }

  def getParticipatedAssistantIds(chatRoom: ChatRoom):Seq[ObjId] = {
    chatRoom.messages.filter{ msg =>
      ( msg.fromType == MessageFromTypes.assistant ) ||
        msg.messageType.contains( models.ChatRooms.MessageType.AssistantDisconnected ) ||
        msg.messageType.contains( models.ChatRooms.MessageType.AssistantJoin ) ||
        msg.messageType.contains( models.ChatRooms.MessageType.AssistantLeave ) ||
        msg.messageType.contains( models.ChatRooms.MessageType.AssistantReconnect )
    }.flatMap{ msg =>

      if( msg.fromType == MessageFromTypes.assistant ){
        Some( msg.from )
      } else {
        val t: Try[Option[ObjId]] = scala.util.Try( ( play.api.libs.json.Json.parse( msg.message ) \ "assistantId" ).asOpt[ObjId] )
        t.getOrElse(None)
      }

    }
  }




  import play.api.libs.concurrent.Execution.Implicits._

  /**
   *
   * @param timeout in milliseconds, 1hr of inactivity by default
   */
  def closeOutdatedChatRooms( timeout:Long = 60*60*1000 ): Unit = {

    val query = Json.obj( "status" -> Json.obj("$ne" -> ChatRoomStatuses.closed), "updated" -> Json.obj( "$exists" -> true, "$lt" -> ( new Date().getTime - timeout ) )  )

    closeChatRoom( query, None, None, Some( models.ChatRooms.CloseReason.UserDisconnected ) )

    val q2 = Json.obj( "status" -> Json.obj("$ne" -> ChatRoomStatuses.closed), "updated" -> Json.obj( "$exists" -> false ), "created" -> Json.obj( "$lt" -> ( new Date().getTime - timeout ) ) )

    closeChatRoom( query, None, None, Some( models.ChatRooms.CloseReason.UserDisconnected ) )
  }

  def closeChatRoom( chatRoomQuery:JsObject, userFeedback:Option[ChatRoomUserFeedback] = None, assistantThatClosed: Option[ObjId] = None, assistantCloseReason:Option[String] = None ) = {

      models.ChatRooms.collection.update(
        chatRoomQuery,
        Json.obj(
          "$set" -> Json.obj(
            "status" -> models.ChatRoomStatuses.closed,
            "closed" -> Some( new Date() ),
            "userFeedback" -> userFeedback,
            "closedByAssistantId" -> assistantThatClosed,
            "assistantCloseReason" -> assistantCloseReason
          )
        )
      )

  }

  def getTestChatRoom(a: Assistant, fakeWidgetId:ObjId = BSONObjectID.generate, fakeUserId:ObjId = BSONObjectID.generate ) = {

    val testChatRoomData =
      models.ChatRoom(
        BSONObjectID.generate,
        models.User(
          fakeUserId,
          "John Doe",
          "user@email.com",
          "User Company",
          fakeWidgetId,
          VisitorStats(
            "127.0.0.1",
            "http://google.com/",
            "google.com",
            Seq("en"),
            "TRACKING",
            Some(UserStatsUserAgent("Firefox", "Windows", "50", "0" )),
            Some(UserStatsOSData("Widnows","10","1")),
            Some("US"),
            Some("New York")
          ),
          None,
          a.companyId
        ),
        fakeWidgetId,
        a.companyId,
        "http://google.com/",
        None,
        Seq(
          ChatRoomMessage( fakeUserId, 0, MessageFromTypes.user, "Hello" ),
          ChatRoomMessage( a.companyId, 1, MessageFromTypes.system, Json.obj("assistantId" -> a._id).toString(), Some( models.ChatRooms.MessageType.AssistantJoin ) ),
          ChatRoomMessage( a._id, 2, MessageFromTypes.assistant, "How are you?" ),
          ChatRoomMessage( fakeUserId, 3, MessageFromTypes.user, "I'm fine, thanks." ),
          ChatRoomMessage( a.companyId, 4, MessageFromTypes.system, "", Some( models.ChatRooms.MessageType.UserDisconnected ) )
        ),
        ChatRoomStatuses.closed,
        Some( ChatRoomUserFeedback(  "user@email.com", "+11212312312", "Nice", problemIsSolved = true ) ),
        None,
        Some( AssistantCloseReason.Resolved ),
        Some(a._id),
        3,
        isDeleted = false
      )

      testChatRoomData
  }



  collection.indexesManager.ensure( Index( Seq( "companyId" -> IndexType.Hashed, "status" -> IndexType.Hashed, "isDeleted" -> IndexType.Hashed ) ) )
  collection.indexesManager.ensure( Index( Seq( "created" -> IndexType.Descending ) ) )
  collection.indexesManager.ensure( Index( Seq( "closed" -> IndexType.Descending ) ) )
  collection.indexesManager.ensure( Index( Seq( "user.stats.trackingCookie" -> IndexType.Hashed ) ) )



}
