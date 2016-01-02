package utils

import play.api.libs.json._

/**
 * User: aloise
 * Date: 08.10.15
 * Time: 0:00
 */
object Json {

  def toOFormat[T](format:Format[T]):OFormat[T] = {
    new OFormat[T] {
      override def reads(json: JsValue): JsResult[T] = format.reads(json)

      override def writes(o: T): JsObject = format.writes(o).asOpt[JsObject].getOrElse(play.api.libs.json.Json.obj())

    }
  }

}
