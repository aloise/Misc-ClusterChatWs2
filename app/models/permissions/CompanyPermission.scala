package models.permissions

import models.base.Collection.ObjId
import models.permissions.CompanyPermission.ValidationError
import play.api.libs.json.{JsBoolean, JsString, JsNumber, JsValue}
import scala.concurrent.{ExecutionContext, Future}
import play.api.libs.json._

import scala.reflect.ClassTag

/**
 * Created by pc3 on 23.09.15.
 */
abstract class CompanyPermission[T]( val name:String ) {

  def validate( companyId:ObjId, newPermissionValue:T, newCountValue:Option[T] = None )(implicit ec:ExecutionContext):Future[Seq[ValidationError]]

  def validate( companyId:ObjId, newPermissionValue:JsValue )(implicit ec:ExecutionContext):Future[Seq[ValidationError]] =
    validate( companyId, this.parse(newPermissionValue) )(ec)

  def parse( value:JsValue ):T

}


object CompanyPermission {

  case class CompanyPermissionItem( name:String, value:JsValue )

  implicit val companyPermissionJsonFormat = Json.format[CompanyPermissionItem]

  case class ValidationError( error:String ) extends Exception(error)
}

trait CompanyPermissionIntParser {

  def parse(value: JsValue): Int = value match {
    case JsNumber( count ) => count.toInt
    case _ => 0
  }

}

trait CompanyPermissionStringParser {

  def parse(value: JsValue): String = value match {
    case JsString( str ) => str
    case _ => ""
  }

}

trait CompanyPermissionBooleanParser {

  def parse(value: JsValue): Boolean = value match {
    case JsBoolean( bool ) => bool
    case _ => true
  }



}

class CompanyPermissionException( permissionName:String, errors:Seq[ValidationError] ) extends Exception( "Permission Error: " + permissionName + " : " + errors.map(_.error).mkString )