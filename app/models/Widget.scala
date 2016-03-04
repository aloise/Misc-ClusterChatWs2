package models


import java.util.Date
import models.ChatRooms._
import play.api.cache.Cache
import play.twirl.api.JavaScript
import reactivemongo.api.indexes.{IndexType, Index}
import reactivemongo.play.json.BSONFormats._
import models.base.Collection
import models.base.Collection.ObjId
import play.api.libs.json.Json
import play.api.Application

import models.ChatRooms._
import reactivemongo.core.commands.Count

import scala.reflect.ClassTag
import models.base.Collection
import models.base.Collection._
import play.api.libs.Crypto
import play.api.mvc.{Result, RequestHeader}
import reactivemongo.api.indexes.{IndexType, Index}
import reactivemongo.bson.{BSONDocument, BSONObjectID}
import play.api.libs.json.Json
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits._
import java.util.Date
import reactivemongo.play.json._
import reactivemongo.play.json.BSONFormats.BSONObjectIDFormat
import play.modules.reactivemongo.json.collection._
import org.cvogt.play.json.implicits.optionNoError

/**
 * User: aloise
 * Date: 15.10.14
 * Time: 22:48
 */
case class Widget (
    _id: ObjId,
    name:String,
    description:String,
    widgetHeader:String,
    companyId:ObjId,
    createdByAssistant:ObjId, // assistant.id
    userGreetingMessage:String,
    widgetSize:String,
    widgetPosition:String,
    widgetColor:String,
    widgetTriggerId:Option[String],
    widgetTriggerTimeout:Option[Int],
    bcSubmitCaseUrl:Option[String] = None,
    bcSiteId:Option[Int] = None,
    isDeleted:Boolean = false,
    emailTemplates:Map[String,Option[String]],
    viewTemplates:Map[String,String],
    created:Date = new Date()
)

import play.api.libs.json.util._

object Widgets extends Collection("widgets", Json.format[Widget]) {

    import play.api.libs.concurrent.Execution.Implicits._
    import play.api.Play.current

    collection.indexesManager.ensure( Index( Seq( "isDeleted" -> IndexType.Hashed ) ) )
    collection.indexesManager.ensure( Index( Seq( "companyId" -> IndexType.Hashed ) ) )

    object TriggerIds {
        val none = "none"
        val widgetLoad = "widgetLoad"
        val widgetHover = "widgetHover"
    }


    object ScriptCache extends CachedEntity[models.Widget] {

        protected def cacheName(s:String)  = "Widget.Script." + s
    }

    object EmbedScriptCache extends CachedEntity[JavaScript] {

        protected def cacheName(s:String) = "Widget.Embed." + s
    }

    def countPerCompany( companyId:ObjId ) = {
        models.Widgets.bsonCollection.count( Some(BSONDocument( "companyId" -> companyId, "isDeleted" -> false )))
    }

}

abstract class CachedEntity[T:ClassTag] {
    protected val widgetCacheExpiration = 60*60*24
    protected def cacheName(s:String):String

    def read(widgetId:String)(implicit app:Application ):Option[T] = {
        Cache.getAs[T](cacheName(widgetId))
    }

    def write( widgetId:String, script:T )(implicit app:Application ) = {
        Cache.set( cacheName(widgetId), script, widgetCacheExpiration )
    }

    def clear( widgetId:String )(implicit app:Application ) = {
        Cache.remove( cacheName(widgetId) )
    }

}
