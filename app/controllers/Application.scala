package controllers

import java.util.{GregorianCalendar, Date}
import javax.xml.datatype.DatatypeFactory
import controllers.UserGateway._
import controllers.helpers.AssistantAuthAction
import controllers.helpers.AuthAction._
import controllers.helpers.JsonResponses._
import models.{Assistant, AssistantRoles}
import play.api._
import play.api.libs.Crypto
import play.api.mvc._
import play.modules.reactivemongo.{ReactiveMongoPlugin, MongoController}
import play.twirl.api.JavaScript
import reactivemongo.bson.{BSONArray, BSONDocument, BSONNull, BSONObjectID}
import models.Widgets._
import play.modules.reactivemongo.json._
import play.modules.reactivemongo.json.BSONFormats.BSONObjectIDFormat
import play.modules.reactivemongo.json.collection._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.Play.current
import play.api.libs.json._
import scala.concurrent.Future
import scala.util.Try


object Application extends BaseController {

  import helpers.HeaderHelpers._

  def index = Action {
    Ok( "Chat App" )
  }

  def crossOriginOptions( path:String ) = Action { r =>
    Ok("").withHeaders( crossOriginHeaders:_* )
  }

  def crossOriginAssets( path: String, file: String) = Action.async { request =>
    Assets.at(path, file)(request).map( _.withHeaders( crossOriginHeaders:_* ) )
  }

}