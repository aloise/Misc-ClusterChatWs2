package models


import java.util.Date
import models.base.Collection
import models.base.Collection.ObjId
import play.api.libs.json.Json
import play.api.mvc.Cookie
import reactivemongo.api.indexes.{IndexType, Index}

import play.modules.reactivemongo.json._
import play.modules.reactivemongo.json.BSONFormats.BSONObjectIDFormat
import play.modules.reactivemongo.json.collection._

import scala.util.Random

/**
 * User: aloise
 * Date: 15.10.14
 * Time: 22:48
 */
case class User (
  _id: ObjId,
  name:String,
  email:String,
  company:String,
  widgetId:ObjId,
  stats: VisitorStats,
  uiUserColorId: Option[Int] = None,
  companyId: ObjId,
  created:Date = new Date()
)

case class VisitorStats(
  ip:String,
  page:String,
  domain:String,
  language:Seq[String],
  trackingCookie:String,
  browser: Option[UserStatsUserAgent],
  os:Option[UserStatsOSData],
  countryCode: Option[String],
  city: Option[String]
)

case class UserStatsUserAgent(
  name: String,
  platform:String,
  majorVersion: String,
  minorVersion: String
)

case class UserStatsOSData(
  name:String,
  majorVersion:String,
  minorVersion: String
)

object UserHelper {

  val trackingCookieName = "bchat-tracking-uid"

  protected val randomColor = new scala.util.Random()

  implicit val userStatsUserAgentJsonFormat = Json.format[UserStatsUserAgent]
  implicit val userStatsOSDataJsonFormat = Json.format[UserStatsOSData]
  implicit val visitorStatsJsonFormat = Json.format[VisitorStats]
  implicit val userJsonFormat = Json.format[User]

  def getTrackingCookie( value:Option[String] = None ) = {

    val newValue = value.getOrElse( Random.alphanumeric.take(32).grouped(8).map( _.mkString ).mkString("-").toLowerCase )

    // Cookie( trackingCookieName, newValue, Some(3600*24*30*12*1), "/", None, false, false )
    Cookie( trackingCookieName, newValue, Some(3600*24*30*12*1), httpOnly = false )
  }

  def getRandomColor = randomColor.nextInt(20)
}

import UserHelper._


/*
object Users extends Collection("users", Json.format[User])   {

  import play.api.libs.concurrent.Execution.Implicits._

  collection.indexesManager.ensure( Index( Seq( "companyId" -> IndexType.Hashed ) ) )
  collection.indexesManager.ensure( Index( Seq( "widgetId" -> IndexType.Hashed ) ) )
  collection.indexesManager.ensure( Index( Seq( "stats.trackingCookie" -> IndexType.Hashed ) ) )

}
*/
