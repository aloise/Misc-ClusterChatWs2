package models

import play.api.libs.json.Json
import reactivemongo.play.json._
import reactivemongo.play.json.BSONFormats.BSONObjectIDFormat
import play.modules.reactivemongo.json.collection._

/**
 * Created by pc3 on 01.09.15.
 */
case class CompanyBrandingSettings (
  logo: Option[String],
  backgroundColor:String,
  menuColor:String,
  menuBorderColor:String,
  activeMenuColor:String,
  hoverMenuColor:String,
  hrColor:String
)

object CompanyBrandsSettingz {
  implicit val jsonFormat = Json.format[CompanyBrandingSettings]
}