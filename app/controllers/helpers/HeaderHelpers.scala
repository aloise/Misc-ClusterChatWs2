package controllers.helpers

/**
 * Created by pc3 on 03.08.15.
 */
object HeaderHelpers {

  val crossOriginHeaders = Seq[(String,String)](
    "Access-Control-Allow-Origin" -> "*",
    "Access-Control-Allow-Origin" -> play.api.Play.current.configuration.getString("assistants.domain").getOrElse("*"),
    "Allow" -> "*",
    "Access-Control-Allow-Methods" -> "POST, GET, PUT, DELETE, OPTIONS",
    "Access-Control-Allow-Credentials" -> "true",
    "Access-Control-Allow-Headers" -> "Origin, X-Requested-With, Content-Type, Accept, Referrer, User-Agent, Cookie, X-Json, X-Prototype-Version",
    "Access-Control-Allow-Credentials" -> "true"

  )

}
