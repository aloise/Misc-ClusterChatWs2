package models

import java.util.Date

import actors.Company.BusinessCatalystTokenRefreshTick
import actors.CompanyMaster.CompanyMessage
import com.ibslabs.bc.OAuth._
import com.ibslabs.bc.models.crm.SiteItem
import models.UserInfos._
import models.base.Collection
import models.base.Collection.ObjId
import models.permissions._
import models.permissions.CompanyPermission._
import models.permissions.CompanyPermission.CompanyPermissionItem
import play.api.libs.json.{JsNull, JsUndefined, JsValue, Json}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import reactivemongo.play.json.BSONFormats._
import CompanyBrandsSettingz.{ jsonFormat => CompanyBrandsSettingsJsonFormat }
import reactivemongo.bson.{BSONString, BSONBoolean, BSONDocument}
import scala.concurrent.{ExecutionContext, Future}
import com.ibslabs.bc.SitesHelper._
import reactivemongo.play.json._
import reactivemongo.play.json.BSONFormats.BSONObjectIDFormat
import play.modules.reactivemongo.json.collection._
import scala.reflect.ClassTag

/**
 * User: aloise
 * Date: 15.10.14
 * Time: 18:34
 */

case class Company(
  _id: ObjId,
  name:String,
  businessCatalystSiteId:Option[String] = None,
  businessCatalystExternalAppSiteId:Option[String] = None,
  businessCatalystAppKey:Option[String] = None,
  businessCatalystAppToken:Option[String] = None,
  businessCatalystSiteItems:Option[Seq[SiteItem]] = None,
  businessCatalystOAuthResponse:Option[ExchangeTokenResponse] = None,
  externalPartnerId:Option[String] = None,
  settings:JsValue = Json.obj(), // { bcOAuthSettings : { clientId, version, bcSecureUrl } , ... }
  permissions:Option[Seq[CompanyPermissionItem]] = None,
  packageName:Option[String] = None,
  branding: Option[CompanyBrandingSettings] = None,
  maxPageVisitsToKeep:Option[Int] = None, // Some(-1) to disable
  created:Date = new Date()
) {

  def checkPermission[T]( newPermissionValue:T, permission:CompanyPermission[T] ):Future[Seq[ValidationError]] = {
    permissions.flatMap{
      _.find( _.name == permission.name ).map { p =>
        permission.validate( _id, newPermissionValue )
      }
    } getOrElse Future.successful( Seq() )
  }

  def checkNewCount[T : Manifest]( newCount:T, permission:CompanyPermission[T] ):Future[Seq[ValidationError]] = {

    permissions.flatMap {
      _.find(_.name == permission.name).map { p =>
        permission.validate(_id, permission.parse( p.value ), Some(newCount))
      }
    } getOrElse Future.successful(Seq())
  }


/*
  def checkPermission[T : Manifest]( newPermissionValue:T, permissionName:String ):Future[Seq[ValidationError]] = {
    permissions.flatMap{
      _.find( _.name == permissionName ).flatMap{ p =>
        Companies.permissionsSet.get( permissionName ).map{
            case permission: CompanyPermission[T] =>
              permission.validate(_id, newPermissionValue)
            case _ =>
              Future.successful(Seq())

        }
      }
    } getOrElse Future.successful(Seq())
  }
*/

/*
  def checkNewCount[T : Manifest]( newCount:T, permissionName:String ):Future[Seq[ValidationError]] = {

    permissions.flatMap{
      _.find( _.name == permissionName ).flatMap{ p =>
        Companies.permissionsSet.get( permissionName ).map{
          case x:CompanyPermission[T] =>
            x.validate(_id, x.parse( p.value ), Some(newCount))
          case _ =>
            Future.successful(Seq())
        }
      }
    } getOrElse Future.successful(Seq())
  }*/

}





case class CompanySettings(
  smtp: Option[CompanySettingsSMTP]
)

case class CompanySettingsSMTP(
  host: String,
  port: Int,
  username:String,
  password:String,
  ssl:Boolean,
  tls:Boolean,
  from:String,
  enabled:Boolean
)

object Companies extends Collection("companies", Json.format[Company]) {

  implicit val companySettingsSMTPJson = Json.format[CompanySettingsSMTP]
  implicit val companyBrandingSettings = Json.format[CompanyBrandingSettings]
  implicit val companySettingsJson = Json.format[CompanySettings]
  import models.Widgets.{ jsonFormat => j0 }

  val MaxPageVisitsToKeepDefault = 10000

  object Permissions {

    val isAnalyticsAvailable = new IsAnalyticsAvailable
    val isArchiveAvailable = new IsArchiveAvailable
    val isBcCrmIntegrationAvailable = new IsBcCrmIntegrationAvailable
    val isWidgetTriggerAvailable = new IsWidgetTriggerAvailable
    val maxUserAgentsCount = new MaxUserAgentsCount
    val maxWidgetsCount = new MaxWidgetsCount

    val all = Seq(
      isAnalyticsAvailable,
      isArchiveAvailable,
      isBcCrmIntegrationAvailable,
      isWidgetTriggerAvailable,
      maxUserAgentsCount,
      maxWidgetsCount
    )

  }

/*
  val permissionsSet:Map[String,CompanyPermission[_]] = permissionSetBoolean ++ Seq(
      ).map { x:CompanyPermission[_] =>
    x.name -> x
  }.toMap
*/



  def refreshOAuthTokens() = {

    val q = Json.obj( "businessCatalystOAuthResponse.refresh_token" -> Json.obj( "$ne" -> JsNull ) )

    models.Companies.collection.find(q).cursor[models.Company]().collect[Seq]().foreach { companies =>

      companies.foreach { company =>
        global.Application.companyMaster ! CompanyMessage( company._id, BusinessCatalystTokenRefreshTick )
      }

    }

  }



  def findByBusinessCatalystSiteId( siteId: String, businessCatalystAppKey:String ):Future[Option[Company]] = {
    collection.find( Json.obj(
      "businessCatalystSiteId" -> siteId,
      "businessCatalystAppKey" -> businessCatalystAppKey
    ) ).one[Company]
  }

  def chartStatsQuery( companyId:ObjId ): Map[String,BSONDocument] = Map(

    "marked_as_resolved_by_client" -> BSONDocument(
      "userFeedback.problemIsSolved" -> BSONBoolean(true),
      "companyId" -> companyId,
      "isDeleted" -> false
    ),
    "marked_as_resolved_by_assistant" -> BSONDocument(
      "assistantCloseReason" -> BSONString(models.ChatRooms.AssistantCloseReason.Resolved),
      "companyId" -> companyId,
      "isDeleted" -> false
    ),
    "marked_as_not_resolved_by_client" -> BSONDocument(
      "userFeedback.problemIsSolved" -> BSONBoolean(false),
      "companyId" -> companyId,
      "isDeleted" -> false
    ),
    "marked_as_spam_by_assistant" -> BSONDocument(
      "assistantCloseReason" -> BSONString(models.ChatRooms.AssistantCloseReason.Spam),
      "companyId" -> companyId,
      "isDeleted" -> false
    ),
    "client_is_not_responding" -> BSONDocument(
      "assistantCloseReason" -> BSONString(models.ChatRooms.AssistantCloseReason.NotResponding ),
      "companyId" -> companyId,
      "isDeleted" -> false
    ),
    "closed_due_to_client_time_out" -> BSONDocument(
      "assistantCloseReason" -> BSONString(models.ChatRooms.CloseReason.UserDisconnected),
      "companyId" -> companyId,
      "isDeleted" -> false
    )
  )


  def validatePermissions( companyId:ObjId, newPermissions: Seq[CompanyPermissionItem] ):Future[Map[String,Seq[ValidationError]]] = {

    val validationFutures =
      newPermissions.map{ p =>
        models.Companies.Permissions.all.
          find( _.name == p.name ).
          map( x => x.validate( companyId, p.value ) ).
          getOrElse(Future.successful(Seq(new CompanyPermission.ValidationError("invalid_permission") ))).
          map{ p.name -> _ }
      }

    Future.sequence( validationFutures ).map( _.toMap )
  }

  def checkPermission[T]( newValue:T, permission:CompanyPermission[T] )(implicit assistant:models.Assistant):Future[Seq[ValidationError]] = {
    assistant.permissions.map{ p =>
        permission.validate( assistant.companyId, newValue )
    } getOrElse Future.successful( Seq() )
  }

  def checkNewCount[T]( newCount:T, permission: CompanyPermission[T] )(implicit assistant:models.Assistant):Future[Seq[ValidationError]] = {

    assistant.permissions.flatMap{
      _.find( _.name == permission.name ).map { p =>
        permission.validate(assistant.companyId, permission.parse(p.value), Some(newCount))
      }
    } getOrElse Future.successful(Seq())
  }

/*
  def checkPermission[T]( newValue:T, permissionName:String )(implicit assistant:models.Assistant):Future[Seq[ValidationError]] = {
    assistant.permissions.flatMap{
      _.find( _.name == permissionName ).flatMap{ p =>
        Companies.permissionsSet.get( permissionName ).map{
          case x:CompanyPermission[T] =>
            x.validate( assistant.companyId, newValue)
          case _ =>
            Future.successful(Seq())
        }
      }
    } getOrElse Future.successful(Seq())
  }

  def checkNewCount[T]( newCount:T, permissionName:String )(implicit assistant:models.Assistant):Future[Seq[ValidationError]] = {

    assistant.permissions.flatMap{
      _.find( _.name == permissionName ).flatMap{ p =>
        Companies.permissionsSet.get( permissionName ).map{
          case x:CompanyPermission[T] =>
            x.validate( assistant.companyId, x.parse( p.value ), Some(newCount))
          case _ =>
            Future.successful(Seq())
        }
      }
    } getOrElse Future.successful(Seq())
  }*/

  def delete(companyId:ObjId)(implicit app:play.api.Application) = {

    val widgets = models.Widgets.collection.find(Json.obj("companyId" -> companyId))
    val widgetCacheCleanFuture =
      widgets.cursor[models.Widget]().collect[Seq]().map{ widgets =>
        widgets.foreach { w =>
          models.Widgets.ScriptCache.clear( w._id.stringify )
          models.Widgets.EmbedScriptCache.clear( w._id.stringify )
        }
        true
      } recover {
        case ex:Throwable =>
          false
      }

    widgetCacheCleanFuture.flatMap { result =>
      val removeSeqFuture =
        Seq(
          models.Companies.collection.remove(Json.obj("_id" -> companyId)),
          models.Assistants.collection.remove(Json.obj("companyId" -> companyId)),
          models.ChatRooms.collection.remove(Json.obj("companyId" -> companyId)),
          models.Pageviews.collection.remove(Json.obj("companyId" -> companyId)),
          models.PageviewByDomains.collection.remove(Json.obj("companyId" -> companyId)),
          models.PageviewByDays.collection.remove(Json.obj("companyId" -> companyId)),
          models.PageviewUniqueByDays.collection.remove(Json.obj("companyId" -> companyId)),
          models.UserInfos.collection.remove(Json.obj("companyId" -> companyId)),
          models.Widgets.collection.remove(Json.obj("companyId" -> companyId))
        )

      Future.sequence( removeSeqFuture )
    }


  }



}
