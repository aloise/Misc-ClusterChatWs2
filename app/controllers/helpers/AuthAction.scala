package controllers.helpers

/**
 * Created by Igor Mordashev <aloise@aloise.name> on 24.10.14.
 */

import controllers.routes
import models.base.Collection.ObjId
import play.api.libs.Crypto
import play.api.libs.json.JsValue
import play.api.mvc._
import play.api.mvc.BodyParsers.parse
import play.api.mvc.BodyParser
import reactivemongo.bson.BSONObjectID
import scala.concurrent.Future
import scala.util.Try
import play.api.libs.concurrent.Execution.Implicits._

class AuthActionClass {

  def withAuth[T]( f: => T => Request[AnyContent] => Result )( implicit getObject: RequestHeader => Future[T], onUnauthorized: ( Throwable, RequestHeader ) => Result ):EssentialAction =
    withAuth[AnyContent,T]( parse.anyContent )( f )( getObject, onUnauthorized )


  def withAuth[A,T]( bodyParser : BodyParser[A] )( f: => T => Request[A] => Result )( implicit getObject: RequestHeader => Future[T], onUnauthorized: ( Throwable, RequestHeader ) => Result ):EssentialAction =
    Action.async( bodyParser) { request =>
      getObject( request ) map { obj =>
        f( obj )( request )
      } recover {
        case t:Throwable =>
          onUnauthorized( t, request )
      } map { _.withHeaders( HeaderHelpers.crossOriginHeaders:_* ) }
    }

  def withAuthJson[T]( f: => T => Request[JsValue] => Result )( implicit getObject: RequestHeader => Future[T], onUnauthorized: ( Throwable, RequestHeader ) => Result ):EssentialAction =
    withAuth[JsValue,T]( parse.json )( f )( getObject, onUnauthorized)

  def withAuthAsync[T]( f: => T => Request[AnyContent] => Future[Result] )( implicit getObject: RequestHeader => Future[T], onUnauthorized: ( Throwable, RequestHeader ) => Result ):EssentialAction =
    withAuthAsync[AnyContent,T]( parse.anyContent )( f )( getObject, onUnauthorized )

  def withAuthAsync[A,T]( bodyParser: BodyParser[A] )( f: => T => Request[A] => Future[Result] )( implicit getObject: RequestHeader => Future[T], onUnauthorized: ( Throwable, RequestHeader ) => Result ):EssentialAction =
    Action.async(bodyParser) { request =>
      getObject( request ) flatMap { obj =>
        f(obj)(request)
      } recover {
        case t:Throwable =>
          onUnauthorized( t, request )
      } map { _.withHeaders( HeaderHelpers.crossOriginHeaders:_* ) }
    }

  def withAuthJsonAsync[T]( f: => T => Request[JsValue] => Future[Result] )( implicit getObject: RequestHeader => Future[T], onUnauthorized: ( Throwable, RequestHeader ) => Result ):EssentialAction =
    withAuthAsync[JsValue,T]( parse.json )( f )( getObject, onUnauthorized)



  def encryptObjId( id:ObjId ):String = {
    Crypto.encryptAES( id.stringify )
  }

  def decryptObjId( str:String ):Try[ObjId] = {
    Try {
      BSONObjectID( Crypto.decryptAES( str ) )
    }
  }


}

object AuthAction extends AuthActionClass