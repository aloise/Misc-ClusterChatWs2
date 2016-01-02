package controllers

import java.net.URLDecoder

import actors.Company.{GetActiveAssistants, NewUserJoin}
import actors.{CompanyMaster, UserConnection, ChatRoom}
import akka.actor.ActorSystem
import controllers.helpers.HeaderHelpers
import controllers.helpers.JsonResponses._
import models.MessageFromTypes._
import models.{ MessageFromTypes, ChatRoomHelper, ChatRoomUserFeedback}
import models.BusinessCatalystOAuth._
import models.base.Collection.ObjId
import play.api.libs.concurrent.Akka
import play.sockjs.api.SockJS.MessageFormatter
import play.modules.reactivemongo.json._
import play.modules.reactivemongo.json.BSONFormats.BSONObjectIDFormat
import play.modules.reactivemongo.json.collection._
import reactivemongo.bson.BSONObjectID

import scala.concurrent.Future
import scala.concurrent.duration._

import play.api.mvc._
import play.api.mvc.Results._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.Play.current

import play.sockjs.api._
import java.util.Date
import reactivemongo.api._
import play.api.libs.concurrent.Execution.Implicits._
import akka.pattern.ask

import scala.util._

/**
 * User: aloise
 * Date: 15.10.14
 * Time: 22:22
 */
object UserGateway extends Controller {

  import actors.messages.SocksMessages._
  import UserConnection.userRequestMessageFormatter
  import ChatRoomHelper._
  import models.Widgets.{ jsonFormat => widgetJsonFormat }
  import models.ChatRooms.{ jsonFormat => j1 }
  import models.Assistants.{ jsonFormat => j2 }


  protected val sendOfflineMessageValidator = (
      ( __ \ "name" ).read[String] and
      ( __ \ "email" ).read[String] and
      ( __ \ "company" ).read[String] and
      ( __ \ "description" ).read[String] and
      ( __ \ "page" ).read[String] and
      ( __ \ "trackingCookie" ).read[String] and
      ( __ \ "widgetId" ).read[BSONObjectID]
  ).tupled

  val sockJSSettings = SockJSSettings.default

  // a default socksjs connector
  lazy val sockjs = SockJSRouter(sockJSSettings).tryAcceptWithActor[UserRequest, Message] { request =>
    // connect the websocket. All processing and error reporting is done inside the UserConnection actor
    Future.successful( Right( UserConnection.getActorProps(request) ) )

  }

  def saveChatFeedback( chatRoomIdStr:String ) = Action(parse.json){ w =>

    ( BSONObjectID.parse(chatRoomIdStr) match {
      case Success( chatRoomId ) =>
        w.body.validate[ChatRoomUserFeedback].map { userFeedback =>
          models.ChatRooms.collection.update(
            Json.obj("_id" -> chatRoomId ),
            Json.obj(
              "$set" -> Json.obj(
                "userFeedback" -> userFeedback
              )

            )
          )
          jsonStatusOk

        }.recoverTotal{ case _ =>
            jsonError("feedback_is_broken")
        }

      case _ =>
        jsonError("chat_room_not_found")
    } ).withHeaders( HeaderHelpers.crossOriginHeaders:_* )



  }

  def userEmailChat( chatRoomIdStr:String, userCookie:String ) = Action.async{ w =>
    jsonErrorFuture("not_implemented").map( _.withHeaders( HeaderHelpers.crossOriginHeaders:_* ) )
  }

  def checkAssistantsOnlineStatus( widgetId: String, ts:String = "" ) = Action.async { w =>
    ( BSONObjectID.parse(widgetId) match {
      case Success( widgetIdObj ) =>

        val params = Json.obj( "_id" -> widgetIdObj, "isDeleted" -> false )

        models.Widgets.collection.find(params).one[models.Widget].flatMap {
          case Some( widget ) =>

            val onlineAssistantsQuery = Json.obj("companyId" -> widget.companyId, "isDeleted" -> false, "status" -> models.Assistants.Status.Online )

            models.Assistants.collection.find( onlineAssistantsQuery ).cursor[models.Assistant]().collect[List]().map{ assistants =>

                jsonStatusOk( Json.obj(
                  "assistants" ->
                    Json.toJson( assistants.map( assistant =>
                      Json.obj(
                        "_id" -> assistant._id,
                        "displayName" -> assistant.displayName,
                        "avatar" -> assistant.avatar
                      )
                    ) ) )
                )

            }


          case None =>
            jsonErrorFuture("widget_not_found")
        }

      case Failure(_) =>
        jsonErrorFuture("widget_id_is_invalid")

    } ).map( _.withHeaders( HeaderHelpers.crossOriginHeaders:_* ) )

  }

  def sendOfflineMessage = Action.async(parse.json) { request =>

    val response =
      request.body.validate( sendOfflineMessageValidator ) map { case ( name, email, company, description, page, trackingCookie, widgetId ) =>

        val userRequestData = RequestUserData( name, email, company, Some(trackingCookie), page )
        val visitorStats = models.Pageviews.getVisitorStats( request, userRequestData.trackingCookie, Some( userRequestData.page ) )
        val params = Json.obj( "_id" -> widgetId, "isDeleted" -> false )

        models.Widgets.collection.find(params).one[models.Widget].flatMap {
          case Some( widget ) =>

            actors.Company.createNewChatRoom( widget, userRequestData, visitorStats, isOffline = true ) flatMap { chatRoomRaw =>

              val chatRoomMessage = models.ChatRoomMessage( chatRoomRaw.user._id, 0, MessageFromTypes.user, description )

              actors.ChatRoom.saveChatRoomMessageinDB( MessageFromTypes.user, chatRoomRaw._id, chatRoomMessage ) map { lastError =>

                  val chatRoom = chatRoomRaw.copy( messages = Seq( chatRoomMessage ) )

                  if( lastError.ok) {
                    jsonStatusOk(Json.obj("chatRoom" -> chatRoom))
                  } else {
                    jsonError("chat_room_creation_failed")
                  }
              }

            }

          case None =>
            jsonErrorFuture("widget_not_found")
        }


      } recoverTotal recoverJsonErrorsFuture

    response.map( _.withHeaders( HeaderHelpers.crossOriginHeaders:_* ) )

  }


}
