package controllers.helpers

import play.api.libs.json._
import play.api.mvc._
import play.api.mvc.Results._
import play.api.libs.concurrent.Execution.Implicits._
import scala.concurrent.Future

/**
 * Created by pc3 on 25.08.15.
 */
object ApiResponses {


  def ApiSuccess( message:Option[String] = None, data: JsValue = Json.obj() ):Future[Result] = {
    Future.successful( Ok(Json.obj("status" -> "success", "message" -> message, "data" -> data) ) )
  }

  def ApiError( message:Option[String] = None, data:JsValue = Json.obj() ):Future[Result] = {
    Future.successful( BadRequest( Json.obj( "status" -> "error", "message" -> message, "data" -> data) ) )
  }

  def ApiError( message:String ):Future[Result] = ApiError( Some(message), Json.obj())

  def ApiError( t:Throwable ):Future[Result] = ApiError( Some(t.getMessage), Json.obj() )

  def ApiJsError( errors:JsError ):Future[Result] =
    ApiError( Some("JSON Validation Error"), JsError.toJson(errors) )

}
