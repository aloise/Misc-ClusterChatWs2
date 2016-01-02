package models

import java.util.Date
import com.netaporter.uri.Uri

import net.sf.uadetector.UserAgentStringParser
import net.sf.uadetector.service.UADetectorServiceFactory
import play.api.mvc.RequestHeader
import reactivemongo.api.QueryOpts
import reactivemongo.api.indexes.{IndexType, Index}
import reactivemongo.bson.{BSONBoolean, BSONString, BSONDocument}
import reactivemongo.core.commands.Count
import play.modules.reactivemongo.json.BSONFormats._
import models.base.Collection
import models.base.Collection.ObjId
import play.api.libs.json.Json
import models.UserHelper._
import com.netaporter.uri.dsl._
import com.netaporter.uri._
import scala.concurrent.Future
import play.modules.reactivemongo.json._
import play.modules.reactivemongo.json.BSONFormats.BSONObjectIDFormat
import play.modules.reactivemongo.json.collection._

import scala.util.Try

/**
 * Created by pc3 on 28.11.14.
 */
case class Pageview(
  _id:ObjId,
  stats: VisitorStats,
  companyId:Option[ObjId] = None,
  widgetId:Option[ObjId] = None,
  created:Date = new Date()
)

case class PageviewByDomain(
  _id:ObjId,
  companyId: ObjId,
  domain:String,
  count: Int
)

case class PageviewByDay(
  _id:ObjId,
  companyId: ObjId,
  domain:String,
  month: Int, // counting from 0..11
  year: Int,
  day:Int,
  count: Int
)

case class PageviewUniqueByDay(
    _id:ObjId,
    companyId: ObjId,
    domain:String,
    cookie:String,
    month: Int, // counting from 0..11
    year: Int,
    day:Int,
    count: Int
)

case class ChatRoomUserStats(
  pageVisits:Seq[Pageview] = Nil,
  pastChatsCount:Int = 0
)

case class GeoIpLocation(
                          countryCode: Option[String],
                          countryName: Option[String],
                          city:Option[String]
                        )

object Pageviews extends Collection("page_views", Json.format[Pageview]) {

  import play.api.libs.concurrent.Execution.Implicits._

  implicit val chatRoomUserStatsToJson = Json.format[ChatRoomUserStats]



  collection.indexesManager.ensure( Index( Seq( "stats.trackingCookie" -> IndexType.Hashed ) ) )
  collection.indexesManager.ensure( Index( Seq( "created" -> IndexType.Descending ) ) )

  def getChatRoomUserStats( trackingCookie:String, count:Int = 5 ):Future[ChatRoomUserStats] = {

    if( trackingCookie.isEmpty ){

      Future.successful( ChatRoomUserStats() )

    } else {

      val pageViewsF = models.Pageviews.collection.find(Json.obj("stats.trackingCookie" -> trackingCookie)).
        options(QueryOpts(0, count)).
        sort(Json.obj("created" -> -1)).
        cursor[Pageview]().
        collect[List](count)

      val pastChatsQueryConditions = BSONDocument( Seq(
        "user.stats.trackingCookie" -> BSONString( trackingCookie ),
        "isDeleted" -> BSONBoolean( false )
      ) )

      val pastChatsCountF = models.ChatRooms.bsonCollection.count( Some(pastChatsQueryConditions) )

      for {
        p <- pageViewsF
        c <- pastChatsCountF
      } yield ChatRoomUserStats(p, c)
    }
  }

  def getVisitorStats( request: RequestHeader, overrideTrackingCookie: Option[String] = None, overridePage: Option[String] = None, geoInfoProvider: String => Option[GeoIpLocation] = (s) => None ):VisitorStats = {

    val (userAgent, operatingSystem ) =
      request.headers.get("User-Agent").map { userAgentStr =>
        val parser:UserAgentStringParser = UADetectorServiceFactory.getResourceModuleParser
        val agent = parser.parse( userAgentStr )

        val ua = Try( UserStatsUserAgent( agent.getName, agent.getOperatingSystem.getFamily.getName, agent.getVersionNumber.getMajor, agent.getVersionNumber.getMinor ) )
        val os = Try( UserStatsOSData( agent.getOperatingSystem.getName, agent.getOperatingSystem.getVersionNumber.getMajor, agent.getOperatingSystem.getVersionNumber.getMinor ) )

        ( ua.toOption, os.toOption  )
      } getOrElse ( None, None )

    // Referer is usually not accessible within the websocket
    val page = overridePage orElse request.headers.get( "Referer" ) orElse request.headers.get("Origin") getOrElse ""

    // TODO - parse languages
    val language = request.headers.get( "Accept-Language" ).getOrElse("").take(2).toLowerCase()
    val trackingCookie = ( request.cookies.get( UserHelper.trackingCookieName ).map(_.value) orElse overrideTrackingCookie ) getOrElse ""
    val domain = Try {
      val uri: Uri = page
      uri.host.getOrElse("")
    }.getOrElse("")

    val location = geoInfoProvider( request.remoteAddress )

    VisitorStats( request.remoteAddress, page, domain, Seq( language ), trackingCookie, userAgent, operatingSystem, location.flatMap( _.countryCode ), location.flatMap( _.city ) )

  }

}

object PageviewByDomains extends Collection("page_view_by_domains", Json.format[PageviewByDomain]) {

}


object PageviewByDays extends Collection("page_view_by_day", Json.format[PageviewByDay]) {

}

object PageviewUniqueByDays extends Collection("page_view_unique_by_day", Json.format[PageviewByDay]) {

}

