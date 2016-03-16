package models


import java.util.Date
import models.Pageviews._
import models.base.Collection
import models.base.Collection.ObjId
import play.api.libs.json.Json
import play.api.mvc.Cookie
import reactivemongo.api.indexes.{IndexType, Index}
import reactivemongo.bson.BSONObjectID
import scala.concurrent.Future

import play.api.Play.current

import reactivemongo.play.json._
import reactivemongo.play.json.BSONFormats.BSONObjectIDFormat
import play.modules.reactivemongo.json.collection._

case class UserInfo(
  _id: ObjId,
  companyId: ObjId,
  notes:String,
  phone:String,
  trackingCookie:String
)

object UserInfos extends Collection("user_info", Json.format[UserInfo]) {

  import play.api.libs.concurrent.Execution.Implicits._
  import models.Companies.{jsonFormat=>j0}

  this.collection.indexesManager.ensure( Index( Seq( "trackingCookie" -> IndexType.Hashed ) ) )

  def findByTrackingCookie( companyId:ObjId, cookie:String ) = {
    this.collection.find( Json.obj( "trackingCookie" -> cookie, "companyId" -> companyId ) ).one[UserInfo]
  }

  def updateUserInfo( companyId:ObjId, cookie: String, userInfo:UserInfo ) = {
    findByTrackingCookie( companyId, cookie ) flatMap {
      case Some( item ) =>
        val infoUpdated = userInfo.copy( _id = item._id, companyId = companyId )
        this.collection.update( Json.obj( "_id" -> item._id ), Json.obj( "$set" -> infoUpdated ) )

      case _ =>
        this.collection.insert( userInfo.copy( _id = BSONObjectID.generate, companyId = companyId ) )
    }
  }





}