package controllers

import controllers.helpers.HeaderHelpers
import play.api.mvc.{Action, Controller, RequestHeader}
import java.net.URLDecoder
import java.util.Date
import models.{UserHelper, UserStatsOSData, UserStatsUserAgent, VisitorStats}
import play.sockjs.api.SockJS.MessageFormatter
import scala.concurrent.Future
import play.modules.reactivemongo.json._
import play.modules.reactivemongo.json.BSONFormats.BSONObjectIDFormat
import play.modules.reactivemongo.json.collection._
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
import models.Widgets.{ jsonFormat => wigetsJsonFormat }
import scala.util.Try
import net.sf.uadetector.service.UADetectorServiceFactory
import net.sf.uadetector.UserAgent
import net.sf.uadetector.UserAgentStringParser
import com.netaporter.uri.dsl._
import com.netaporter.uri._
import controllers.helpers.JsonResponses._


/**
 * Created by pc3 on 02.12.14.
 */
object Tracker extends Controller {

  case class TrackerData(
    trackingCookie:Option[String] = None
  )

  protected val trackerDataToJson = Json.format[TrackerData]

  def widget(widgetId:String, trackingCookie:String, ts:String = "") = Action { request =>

    val trackingCookieOpt = if( trackingCookie.trim.isEmpty) None else Some(trackingCookie.trim)
    val cookie = UserHelper.getTrackingCookie( request.cookies.get( UserHelper.trackingCookieName ).map(_.value) orElse trackingCookieOpt )

    Ok(Json.obj( "result" -> "ok", "cookie" -> Json.obj( cookie.name -> cookie.value ) ) ).
      withCookies( cookie ).withHeaders( HeaderHelpers.crossOriginHeaders:_* )

  }

}
