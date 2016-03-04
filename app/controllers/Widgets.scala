package controllers

import java.net.URLEncoder
import controllers.helpers.ApiResponses._
import models.{Widget, UserHelper, Assistant}
import models.permissions._
import play.api.mvc._
import play.api.libs.json._
import play.api.{Logger, Play}
import play.api.cache.Cache
import play.twirl.api.JavaScript
import reactivemongo.bson.{BSONDocument, BSONObjectID}
import reactivemongo.play.json._
import reactivemongo.play.json.BSONFormats.BSONObjectIDFormat
import play.modules.reactivemongo.json.collection._
import reactivemongo.core.commands.Count
import models.base.Collection
import play.api.libs.concurrent.Execution.Implicits._
import models.base.Collection.ObjId
import play.api.Play.current

import scala.concurrent.Future
import scala.util._

import controllers.helpers.{AuthAction, AssistantAuthAction}
import controllers.helpers.AssistantAuthAction.Implicits._
import controllers.helpers.AuthAction._
import controllers.helpers.JsonResponses._
import play.api.libs.Crypto
import play.api.mvc.{ Action, Controller, Cookie, RequestHeader }
import play.api.libs.functional.syntax._
import play.api.libs.json._
import scala.concurrent.Future
import java.util.Date
import models.Widgets.{jsonFormat => widgetsJsonFormat, TriggerIds}

/**
 * Created by aloise on 17.10.14.
 */

object Widgets extends Controller {

  import models.Companies.{ jsonFormat => j0 }

  def embed( widgetId:String ) = Action.async { request =>

    // get the referer
    val refererOpt = request.getQueryString("refererURL")

    // track the visitor in async manner
      val widgetContent: Future[Option[play.twirl.api.Html]] = BSONObjectID.parse( widgetId ).map { widgetObjId =>

      val widgetCache = models.Widgets.ScriptCache.read( widgetObjId.stringify )

      widgetCache.map[Future[Option[play.twirl.api.Html]]]{ widget =>
          val html: play.twirl.api.Html = getWidgetJavascriptHtml(widget, refererOpt)(request)
          Future.successful(Some(html))
        } getOrElse {

          val queryConditions = BSONDocument("_id" -> widgetObjId, "isDeleted" -> false)

          models.Widgets.collection.find( queryConditions ).one[models.Widget].map {

            case Some(widget) =>

              val html: play.twirl.api.Html = getWidgetJavascriptHtml(widget, refererOpt)(request)
              models.Widgets.ScriptCache.write( widgetObjId.stringify, widget )
              Some(html)

            case None =>

              None

          }

        }
      } match {
        case Success( contentFuture ) =>
          contentFuture
        case Failure( ex ) =>
          Future.failed(ex)
      }

    widgetContent.map {
      case Some(content) =>
        Ok( content )
      case None =>
        // widget id was not found
        BadRequest("Widget Not Found")
    } recover {
      case ex:Throwable =>

        Logger.debug( "Widget Load Error #" + widgetId + " " + ex.getMessage )
        InternalServerError("Widget Load Error")
    } map { response =>
      response.withHeaders( "X-UA-Compatible" -> "IE=8", "P3P" -> "CP=\"NON DSP LAW CUR ADM DEV TAI PSA PSD HIS OUR DEL IND UNI PUR COM NAV INT DEM CNT STA POL HEA PRE LOC IVD SAM IVA OTC\""  )
    }


  }


  def embedIframe( widgetId:String ) = Action.async { request =>
    // track the visitor in async manner
    val widgetContent: Future[Option[JavaScript]] = BSONObjectID.parse( widgetId ).map { widgetObjId =>

      // val widgetContentCache = models.Widgets.EmbedScriptCache.read( widgetObjId.stringify )
      val widgetCache = models.Widgets.ScriptCache.read( widgetObjId.stringify )

      widgetCache.map[Future[Option[JavaScript]]]{ widget =>
        val js: JavaScript = getWidgetIframeJavascript(widget)(request)
        Future.successful(Some(js))
      } getOrElse {

        val queryConditions = BSONDocument("_id" -> widgetObjId, "isDeleted" -> false)

        models.Widgets.collection.find( queryConditions ).one[models.Widget].map {

          case Some(widget) =>

            val js: JavaScript = getWidgetIframeJavascript(widget)(request)
            models.Widgets.ScriptCache.write( widgetObjId.stringify, widget )
//            models.Widgets.EmbedScriptCache.write( widgetObjId.stringify, js )
            Some(js)

          case None =>

            None

        }

      }
    } match {
      case Success( contentFuture ) =>
        contentFuture
      case Failure( ex ) =>
        Future.failed(ex)
    }

    widgetContent.map {
      case Some(content) =>
        Ok( content )
      case None =>
        // widget id was not found
        BadRequest("Widget Not Found")
    } recover {
      case ex:Throwable =>

        Logger.debug( "Widget Load Error #" + widgetId + " " + ex.getMessage )
        InternalServerError("Widget Load Error")
    } map { response =>
      response.withHeaders( "Expires" -> "-1", "Cache-Control" -> "private, max-age=0, no-store, no-cache, must-revalidate, post-check=0, pre-check=0", "Pragma" -> "no-cache", "P3P" -> "CP=\"NON DSP LAW CUR ADM DEV TAI PSA PSD HIS OUR DEL IND UNI PUR COM NAV INT DEM CNT STA POL HEA PRE LOC IVD SAM IVA OTC\"" )
    }
  }

  protected def getWidgetJavascriptHtml( widget: models.Widget, refererOpt:Option[String] )( implicit r:RequestHeader ) :play.twirl.api.Html = {
    val widgetConfig = Json.obj(
      "socksGateway" -> JsString( "gateway/user" ),
      "widgetHeader" -> widget.widgetHeader,
      "widgetSize" -> widget.widgetSize,
      "widgetPosition" -> widget.widgetPosition,
      "widgetColor" -> widget.widgetColor,
      "widgetTriggerId" -> widget.widgetTriggerId,
      "widgetTriggerTimeout" -> widget.widgetTriggerTimeout,
      "baseURL" -> JsString( routes.Application.index().absoluteURL(true)(r).toString ),
      "resourceURL" -> JsNull, // JsString( "https://" + global.Application.getBucketName + ".s3.amazonaws.com/" ),
      "refererURL" -> refererOpt,
      "services" -> Json.obj(
        "saveChatFeedback" -> routes.UserGateway.saveChatFeedback("$chatRoomId").absoluteURL(true)(r).toString
      ),
      "viewTemplates" -> widget.viewTemplates
    )

    views.html.Widget.scripts.widget( widget._id.stringify, widgetConfig )

  }


  protected def getWidgetIframeJavascript( widget: models.Widget )( implicit r:RequestHeader ) :play.twirl.api.JavaScript = {

    val refererOpt = r.headers.get("referer").flatMap { refererUrl =>
      Try( new java.net.URL( refererUrl ) ).map { url =>
        url.getProtocol + "://" + url.getHost + (if ( url.getPort == -1) "" else ":" + url.getPort)
      }.toOption
    }

    val iframeURL =
      routes.Widgets.embed( widget._id.stringify ).absoluteURL(true)(r).toString +
      refererOpt.fold(""){ referer =>
        "?refererURL=" + URLEncoder.encode( referer, "UTF-8" )
      }


    val widgetConfig = Json.obj(
      "widgetSize" -> widget.widgetSize,
      "widgetPosition" -> widget.widgetPosition,
      "baseURL" -> JsString( routes.Application.index().absoluteURL(true)(r).toString ),
      "iframeURL" -> JsString( iframeURL ),
      "refererURL" -> refererOpt,
      "widgetTriggerId" -> widget.widgetTriggerId,
      "widgetTriggerTimeout" -> widget.widgetTriggerTimeout,
      "viewTemplates" -> widget.viewTemplates
    )

    views.js.Widget.scripts.widgetIframe( widget._id.stringify, widgetConfig )

  }


}
