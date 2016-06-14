package controllers.helpers

import models.Assistant
import models.base.Collection.ObjId
import play.api.mvc._
import reactivemongo.bson.BSONObjectID
import scala.concurrent.Future
import play.api.libs.json._
import play.api.mvc.Results._
import models.Assistants.jsonFormat
import play.api.libs.concurrent.Execution.Implicits._
import reactivemongo.play.json._
import reactivemongo.play.json.BSONFormats.BSONObjectIDFormat
import play.modules.reactivemongo.json.collection._
import scala.language.implicitConversions

/**
 * Created by Igor Mordashev <aloise@aloise.name> on 24.10.14.
 */
object AssistantAuthAction {

  import models.permissions.CompanyPermission.{ companyPermissionJsonFormat, CompanyPermissionItem }

  val cookieMaxAge:Int = 30*24*3600 // 30 days

  val assistantAuthCookieName = "assistantId"
  val assistantAuthCookiePath = "/"

  case class CompanyPermissions(
    _id:ObjId,
    permissions:Option[Seq[CompanyPermissionItem]]
  )


  implicit val companyPermissionJsonFormat = Json.format[CompanyPermissions]

  def getAuthCookie( assistant:models.Assistant, rememberMe:Boolean = false ):Cookie = {
    Cookie( assistantAuthCookieName, AuthAction.encryptObjId( assistant._id ), if( rememberMe ) Some(cookieMaxAge) else None, assistantAuthCookiePath, httpOnly = false )
  }

  object Implicits {
    implicit def getObject( request:RequestHeader ): Future[models.Assistant] = {

      val bsonAssistantIdOpt = request.cookies.get( assistantAuthCookieName ).flatMap { cookie =>
        AuthAction.decryptObjId( cookie.value ).toOption
      }

      bsonAssistantIdOpt.map{ bsonAssistantId =>

        models.Assistants.collection.find( Json.obj( "_id" -> bsonAssistantId, "isDeleted" -> false ) ).one[models.Assistant].flatMap {
          case Some(assistant) =>

            val companyPermissions = models.Companies.collection.find( Json.obj( "_id" -> assistant.companyId ), Json.obj("permissions" -> 1 ) ).one[CompanyPermissions]

            companyPermissions.map {
              case Some(p) =>
                assistant.copy(permissions = p.permissions)

              case _ =>
                throw new Exception("Company Not Found")

            }

          case None =>
            throw new Exception("Assistant Not Found")
        }

      }.getOrElse( Future.failed( new Exception( "Incorrect Cookie" ) ) )

    }

    implicit def onUnauthorized( t:Throwable, request: RequestHeader ):Result = {
      Unauthorized("Unathorized: " + t.getMessage )
    }
  }



}
