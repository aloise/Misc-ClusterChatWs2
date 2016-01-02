package controllers.helpers

/**
 * Created by Igor Mordashev <aloise@aloise.name> on 24.10.14.
 */

import play.api.mvc._
import models.Assistant
import play.api.libs.json._
import play.api.libs.functional.syntax._
import scala.concurrent.Future
import play.api.mvc.BodyParsers.parse
import play.api.mvc.BodyParser
import reactivemongo.bson.BSONObjectID
import scala.concurrent.Future
import scala.util.Try
import play.api.libs.concurrent.Execution.Implicits._
import play.api.mvc.Results._


object JsonResponses {

  implicit val jsonThrowableWriter = new Writes[Throwable]{
    def writes(ex: Throwable) = Json.obj(
      "message" -> ex.getClass.getName.replace("$", "."),
      "description" -> ex.getMessage,
      "status" -> "error"
    )
  }


  def recoverJsonErrorsFuture( errors:JsError ):Future[Result] =
    Future.successful( BadRequest(Json.obj("status" ->"error", "message" -> JsError.toJson(errors))) )

  def recoverJsonErrorsFuture( error:String, description: String = null ):Future[Result] =
    Future.successful( BadRequest(Json.obj("status" ->"error", "message" -> error, "description" -> description )))

  def recoverJsonExceptionFuture( error:Throwable ):Future[Result] =
    Future.successful( BadRequest( Json.toJson( error )) )

  def recoverJsonException( error:Throwable ):Result =
    BadRequest( Json.toJson(error) )

  def recoverJsonErrors( errors:JsError ):Result =
    recoverJsonErrors( errors, Json.obj())


  def recoverJsonErrors( errors:JsError, additionalErrorObj : JsObject ):Result =
    BadRequest(Json.obj( "status" -> "error", "message" -> JsError.toJson(errors)) ++ additionalErrorObj )

  def recoverJsonErrors( error:String, description: String = null ):Result =
    recoverJsonErrors(error, Json.obj( "description" -> description ))

  def recoverJsonErrors( error:String, additionalErrorObj : JsObject ):Result =
    BadRequest(Json.obj( "status" -> "error", "message" -> error ) ++ additionalErrorObj )


  def jsonStatusOk:Result = jsonStatusOk(Json.obj())

  def jsonStatusOkFuture = Future.successful( jsonStatusOk )

  def jsonStatusOk( additionalData : JsObject ) = Ok(Json.obj("status" -> "ok") ++ additionalData)

  def jsonStatusOkFuture( additionalData : JsObject ) = Future.successful( Ok(Json.obj("status" -> "ok") ++ additionalData) )

  def jsonError( error:String ) = BadRequest( Json.obj("status" -> "error", "message" -> error ) )

  def jsonErrorFuture( error:String ) = Future.successful( jsonError(error) )

}
