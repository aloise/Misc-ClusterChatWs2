/**
 * Created by Igor Mordashev <aloise@aloise.name> on 17.10.14.
 */

import actors.CompanyMaster
import akka.actor.Props
import play.api._
import play.api.libs.Crypto
import play.api.mvc._
import play.libs.Akka
import play.modules.reactivemongo.ReactiveMongoPlugin
import play.api.libs.concurrent.Akka

object Global extends WithFilters with GlobalSettings  {

  override def onStart(app: Application): Unit = {

    models.ChatRooms.closeOutdatedChatRooms()

    actors.Company.refreshOAuthTokens()

  }

}