package models.permissions

import models.base.Collection._
import models.permissions.CompanyPermission.ValidationError

import scala.concurrent.{Future, ExecutionContext}

/**
  * User: aloise
  * Date: 05.12.15
  * Time: 23:11
  */
class DefaultPermissionInt extends CompanyPermission[Int]("default_int") with CompanyPermissionIntParser {

  override def validate( companyId:ObjId, newPermissionValue: Int, newCountValue:Option[Int] = None )(implicit ec:ExecutionContext): Future[Seq[ValidationError]] = {
    Future.successful( Seq() )
  }

}

class DefaultPermissionBoolean extends CompanyPermission[Boolean]("default_boolean") with CompanyPermissionBooleanParser {

  override def validate( companyId:ObjId, newPermissionValue: Boolean, newCountValue:Option[Boolean] = None )(implicit ec:ExecutionContext): Future[Seq[ValidationError]] = {
    Future.successful( Seq() )
  }

}
