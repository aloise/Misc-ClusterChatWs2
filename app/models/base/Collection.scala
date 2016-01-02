package models.base

/**
 * Created by Igor Mordashev <aloise@aloise.name> on 17.10.14.
 */
import play.api.Play.current
import play.api.libs.json._
import reactivemongo.api._
import reactivemongo.api.collections._

import play.api.libs.concurrent.Execution.Implicits.defaultContext

import play.modules.reactivemongo.ReactiveMongoApi

import reactivemongo.api.{ DB, MongoConnection, MongoDriver }
import reactivemongo.bson.BSONObjectID

import scala.reflect.ClassTag

// Reactive Mongo plugin, including the JSON-specialized collection
import play.modules.reactivemongo.json.collection.{ JSONCollection, JSONQueryBuilder }
import reactivemongo.api.collections.bson.BSONCollection

abstract class Collection[T:ClassTag]( val name:String, fmt:Format[T] ) {

  implicit val jsonFormat:OFormat[T] = utils.Json.toOFormat( fmt )

  lazy val reactiveMongoApi:ReactiveMongoApi = current.injector.instanceOf[ReactiveMongoApi]

  lazy val db = reactiveMongoApi.db

  lazy val collection: JSONCollection = reactiveMongoApi.db.collection[JSONCollection](name)

  lazy val bsonCollection: BSONCollection = reactiveMongoApi.db.collection[BSONCollection](name)

}

object Collection {
  type ObjId = BSONObjectID

}
