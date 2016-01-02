package controllers

import java.net.URLDecoder

import actors.{UserConnection, AssistantConnection}
import actors.messages.SocksMessages.{AssistantRequest, Message}
import play.api.libs.json.{JsString, Json}
import play.api.mvc.{RequestHeader, Controller}
import play.sockjs.api.{SockJSRouter, SockJSSettings}
import play.modules.reactivemongo.json._
import play.modules.reactivemongo.json.BSONFormats.BSONObjectIDFormat
import play.modules.reactivemongo.json.collection._
import scala.concurrent.Future
import scala.util.Try

import actors.messages.SocksMessages._
import AssistantConnection.assistantRequestMessageFormatter
import models.Widgets.{ jsonFormat => widgetJsonFormat }

import play.api.Play.current

/**
 * User: aloise
 * Date: 02.11.14
 * Time: 9:42
 */
object AssistantGateway  extends Controller  {

  val sockJSSettings = SockJSSettings.default

  lazy val sockjs = SockJSRouter(sockJSSettings).tryAcceptWithActor[AssistantRequest, Message] { request =>

    // connect the websocket. All processing and error reporting is done inside the UserConnection actor
    Future.successful( Right( AssistantConnection.getActorProps( request ) ) )

  }


  protected def getRequestCookies( request:RequestHeader ) = {
    request.cookies.map{ c =>
      val str = URLDecoder.decode( c.value, "UTF-8")

      c.name -> str // Try( Json.parse( str ) ).getOrElse( JsString(str) )

    }.toMap
  }



}
