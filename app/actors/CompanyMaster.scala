package actors

import akka.actor.Actor.Receive
import akka.actor._
import models.base.Collection.ObjId
import play.api.libs.json._
import reactivemongo.play.json.BSONFormats._
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits._
import reactivemongo.play.json._
import reactivemongo.play.json.BSONFormats.BSONObjectIDFormat
import play.modules.reactivemongo.json.collection._

/**
 * User: aloise
 * Date: 18.10.14
 * Time: 23:35
 */
class CompanyMaster extends Actor {
  import CompanyMaster._

  import models.Companies.{ jsonFormat => companiesJsonFormat }

  var companies:Map[ ObjId, (ActorRef, models.Company)] = Map()

  override def receive: Receive = {

    case CompanyMessage( companyId, message ) =>

      val msg = message

      companies.get( companyId ).fold[Unit] {
        // company not found - create it
        createCompanyActor( companyId, Some(message) )

      } { case ( actor, _ ) =>
        actor forward msg
      }

    case UpdateCompanyData( actor, data ) =>

        companies = companies + ( data._id -> ( actor, data ) )

    case DeleteCompany( companyId ) =>

      companies.get( companyId ).foreach{ case (actor,_) =>
        // actor should kill itself
        actor ! actors.Company.CompanyDeleted( )
        // actor ! PoisonPill
      }
      companies = companies - companyId

    case Terminated( actor ) =>
      companies.
        find{ case ( _, ( a, _ ) ) => a == actor }.
        foreach{ case ( _, ( a, c ) ) =>
            companies = companies - c._id
        }
  }

  protected def createCompanyActor( companyId: ObjId, sendMessage:Option[Any] = None, replyTo:ActorRef = context.sender() ):Future[ (ActorRef, models.Company ) ] = {

    models.Companies.collection.find( Json.obj( "_id" -> companyId ) ).one[models.Company].map {
      case Some( company ) =>
        val actor = companyActorProps( company )

        // forward messages
        sendMessage.foreach{ msg =>
          actor.tell( msg, replyTo )
        }

        // me ! UpdateCompanyData( actor, company )
        companies = companies + ( company._id -> ( actor, company ) )

        ( actor, company )
      case None =>
        throw new Exception("Company not found")
    }

  }

  protected def companyActorProps( company:models.Company ) = {
    context.actorOf( Props( classOf[actors.Company], company ) )
  }
}

object CompanyMaster {

  case class CompanyMessage( companyId:ObjId, message:Any )

  case class DeleteCompany( companyId:ObjId )

//  case class UpdateCompany( company:models.Company )

  case class UpdateCompanyData( actor:ActorRef,  company:models.Company )

}
