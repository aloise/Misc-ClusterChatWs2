package models

import java.awt.Color
import java.io.{ByteArrayOutputStream, FileInputStream, File}

import com.sksamuel.scrimage.Image
import com.sksamuel.scrimage.nio.PngWriter
import controllers.helpers.S3File
import models.ChatRooms._
import models.base.Collection
import models.base.Collection._
import models.permissions.CompanyPermission.CompanyPermissionItem
import play.api.libs.Crypto
import play.api.mvc.{Result, RequestHeader}
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.api.indexes.{IndexType, Index}
import reactivemongo.bson._
import play.api.libs.json.{JsString, Json}
import reactivemongo.play.json.BSONFormats._
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits._
import java.util.Date
import scala.util._
import reactivemongo.play.json._
import reactivemongo.play.json.BSONFormats.BSONObjectIDFormat
import play.modules.reactivemongo.json.collection._

/**
 * User: aloise
 * Date: 15.10.14
 * Time: 22:50
 */
case class Assistant(
  _id:ObjId,
  username:String,
  displayName:String,
  email:String,
  assistantRole:String,
  avatar:Option[String] = None,
  password:String,
  companyId: ObjId,
  lastLogin: Option[Date] = None,
  status:String = models.Assistants.Status.Away,
  statusBeforeDisconnect:Option[String] = None, //  = models.Assistants.Status.Away,
  isDeleted: Boolean = false,
  permissions:Option[Seq[CompanyPermissionItem]] = None,
  passwordResetToken:Option[String] = None,
  created : Date = new Date()
)

case class AssistantInfo(
  _id:ObjId,
  displayName:String,
  avatar:Option[String] = None
)

import play.api.libs.json.util._

object AssistantRoles {
  val administrator = "administrator"
  val operator = "operator"

}

object Assistants extends Collection("assistants", Json.format[Assistant]) {


  implicit val assistantInfoJsonFormat = Json.format[AssistantInfo]

  object Status {
    val Online = "online"
    val Away = "away"
  }

  object AvatarImage {
    val width = 36
    val height = 36
    val color = com.sksamuel.scrimage.Color.White
    val format = com.sksamuel.scrimage.Format.PNG
    val contentType = "image/png"
    val extension = "png"
  }

  val millisecondsInDay:Long = 3600*24*1000

  def authorize( usernameOrEmail:String, password:String, companyId:Option[BSONObjectID] = None ):Future[Option[models.Assistant]] = {

    val opts = Json.obj(
      "$or" -> Json.arr(
        Json.obj( "email" -> usernameOrEmail ),
        Json.obj( "username" -> usernameOrEmail )
      ),
      "password" -> Crypto.sign( password ),
      "isDeleted" -> false
    )

    models.Assistants.collection.find(
      opts ++ companyId.fold( Json.obj() )( cid => Json.obj( "companyId" -> companyId ) )
    ).one[models.Assistant]
  }

  def create( a:Assistant ) = {
    models.Assistants.collection.insert(a)
  }

  def countPerCompany( companyId:ObjId ) = {
    models.Assistants.bsonCollection.count( Some(BSONDocument( "companyId" -> companyId, "isDeleted" -> false )))
  }

  def checkUsernameIsUnique( username:String , assistantId:Option[ObjId] = None ):Future[Boolean] = {

    val conditions =
      Json.obj( "username" -> username, "isDeleted" -> false ) ++
      assistantId.map( id => Json.obj( "_id" -> Json.obj( "$ne" -> id ) ) ).getOrElse(Json.obj())

    collection.find( conditions ).one[models.Assistant].map {
      _.isEmpty
    }
  }

  def checkEmailIsUnique( username:String , assistantId:Option[ObjId] = None ):Future[Boolean] = {

    val conditions =
      Json.obj( "email" -> username, "isDeleted" -> false ) ++
        assistantId.map( id => Json.obj( "_id" -> Json.obj( "$ne" -> id ) ) ).getOrElse(Json.obj())


    collection.find( conditions ).one[models.Assistant].map {
      _.isEmpty
    }
  }

  def uploadAvatar( tempFile: File ):Future[String] = {
    Try( new FileInputStream( tempFile ) ) match {
      case Success( stream ) =>


        val f = Future {
          val image = Image.fromStream( stream ).fit( AvatarImage.width, AvatarImage.height, AvatarImage.color )

          val byteStream = new ByteArrayOutputStream()

          PngWriter().write( image, byteStream )

          byteStream

        }

        f.flatMap { stream =>
          val filename = java.util.UUID.randomUUID.toString  + "." + AvatarImage.extension

          val path = Assistants.name + "/avatars/" + filename

          S3File.uploadFile( path , AvatarImage.contentType, stream.toByteArray ).map { _ =>
            path
          }

        }



      case Failure(ex) =>
        Future.failed(ex)
    }
  }


  def reportAverageChatsPerDayQuery(assistantId:ObjId) = {

    import models.ChatRooms.bsonCollection.BatchCommands.AggregationFramework.{ SumValue, Group, Match, SumField }

    models.ChatRooms.bsonCollection.aggregate(
      // filter chats from assistant
      Match( BSONDocument(
        "messages" -> BSONDocument(
          "$elemMatch" -> BSONDocument(
            "fromType" -> BSONInteger( MessageFromTypes.assistant ),
            "from" -> assistantId
          )
        ),
        "isDeleted" -> BSONBoolean(false)
      ) ),

      List[ BSONCollection#PipelineOperator ](
        // group by day - count chats every day
        Group( BSONDocument(
          // { $subtract:[ { $divide: [ "$created", 3600*24*1000 ] }, { $divide:[ { $mod: [ "$created", 3600*24*1000 ] }, 3600*24*1000 ] } ] },
          "$subtract" -> BSONArray(
            BSONDocument( "$divide" -> BSONArray( "$created", BSONLong( millisecondsInDay ) ) ),
            BSONDocument( "$divide" -> BSONArray( BSONDocument( "$mod" -> BSONArray( "$created", BSONLong( millisecondsInDay ) ) ), BSONLong( millisecondsInDay ) ) )
          )

        ) )( "chatsPerDayCount" -> SumValue(1) ),
        // calculate average per day
        Group( BSONNull )(
          "daysTotal" -> SumField("chatsPerDayCount")
        )
      )
    )

  }

  def reportChatsTotalQuery(assistantId:ObjId) = {

    import models.ChatRooms.bsonCollection.BatchCommands.AggregationFramework.{ SumValue, Group, Match  }

    models.ChatRooms.bsonCollection.aggregate(
      Match(BSONDocument(
        "messages" -> BSONDocument(
          "$elemMatch" -> BSONDocument(
            "fromType" -> BSONInteger(MessageFromTypes.assistant),
            "from" -> assistantId
          )
        ),
        "isDeleted" -> BSONBoolean(false)
      )),
      List(
        Group(BSONNull)("chatsTotal" -> SumValue(1))
      )
    )

  }

  def reportMedianInitialResponseQuery(assistantId:ObjId) = {
    import models.ChatRooms.bsonCollection.BatchCommands.AggregationFramework.{ Avg, Group, Match, Unwind, Sort, Project, First, Ascending }

    models.ChatRooms.bsonCollection.aggregate(
      Match( BSONDocument(
        "messages" -> BSONDocument(
          "$elemMatch" -> BSONDocument(
            "fromType" -> BSONInteger( MessageFromTypes.assistant ),
            "from" -> assistantId
          )
        ),
        "isDeleted" -> BSONBoolean(false)
      ) ),
      List(

        Unwind( "messages" ),
        Match( BSONDocument( "messages.fromType" -> BSONInteger( MessageFromTypes.assistant ), "messages.from" -> assistantId ) ),
        Sort( Ascending( "_id" ), Ascending( "messages.sequenceNum" ) ),
        // { _id: "$_id", msg: { $first: "$messages.sequenceNum" }, chatCreated: { $first: "$created" }, replied : { $first : "$messages.created" }  } },
        Group( BSONString("$_id") )( "chatCreated" -> First("created"), "replied" -> First( "messages.created") ),
        // { delta: { $subtract: [ "$messages.created", "$created" ] } }
        Project( BSONDocument( "delta" -> BSONDocument( "$subtract" -> BSONArray( BSONString( "$replied" ), BSONString(  "$chatCreated" ) ) ) ) ),
        Group( BSONNull )( "average" -> Avg( "delta" ) )
      )

    )
  }

  def afterSuccessfulLogin(assistant:models.Assistant) = {

    models.Assistants.collection.update(
      Json.obj( "_id" -> assistant._id ),
      Json.obj( "$set" ->
        Json.obj(
          "lastLogin" -> new Date(),
          "status" -> JsString( assistant.statusBeforeDisconnect.getOrElse( models.Assistants.Status.Away ) )
        )
      )
    ) flatMap { error =>
      if( error.ok ){
        models.Assistants.collection.find(Json.obj("_id" -> assistant._id )).one[models.Assistant] map {
          case Some( assistant ) =>
            assistant
          case _ =>
            throw new Exception("assistant_not_found")
        }

      } else {
        throw new Exception("assistant_not_found")
      }
    }

  }


  def companyAssistantsInfo( companyId:ObjId) = {
    val assistantsQuery = Json.obj( "companyId" -> companyId, "isDeleted" -> false )
    models.Assistants.collection.
      find( assistantsQuery, Json.obj( "_id" -> 1, "displayName" -> 1, "avatar" -> 1 ) ).
      cursor[AssistantInfo]().
      collect[Seq]()
  }


  collection.indexesManager.ensure( Index( Seq( "username" -> IndexType.Hashed ) ) )
  collection.indexesManager.ensure( Index( Seq( "password" -> IndexType.Hashed ) ) )
  collection.indexesManager.ensure( Index( Seq( "email" -> IndexType.Hashed ) ) )

}
