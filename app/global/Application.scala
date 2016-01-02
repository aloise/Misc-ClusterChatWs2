package global

import actors.CompanyMaster
import akka.actor.Props
import controllers.helpers.HeaderHelpers
import play.api.libs.concurrent.Akka
import play.api.Play.current
import org.apache.commons.io.IOUtils


/**
 * User: aloise
 * Date: 19.10.14
 * Time: 0:57
 */
object Application {

  val appName = "chat-cluster-ws"
  val appVersion = "1.0.0"

  lazy val companyMaster = Akka.system.actorOf( Props( classOf[CompanyMaster] ) )


}
