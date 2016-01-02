/**
 * Created by Igor Mordashev <aloise@aloise.name> on 17.10.14.
 */

import actors.CompanyMaster
import akka.actor.Props
import play.api._
import play.api.libs.Crypto
import play.api.mvc._
import play.filters.gzip.GzipFilter
import play.libs.Akka
import play.modules.reactivemongo.ReactiveMongoPlugin
import play.api.libs.concurrent.Akka


object GlobalHelpers {
  def shouldGzip(request:RequestHeader, response:ResponseHeader) = {
    response.headers.get("Content-Type").exists(_.startsWith("text/javascript"))
  }
}

object Global extends WithFilters(new GzipFilter( shouldGzip = GlobalHelpers.shouldGzip )) with GlobalSettings  {

  override def onStart(app: Application): Unit = {

    models.ChatRooms.closeOutdatedChatRooms()

    actors.Company.refreshOAuthTokens()

  }

}