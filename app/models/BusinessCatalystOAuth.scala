package models

import play.api.libs.json.{JsValue, Json}
import java.util.Date
/**
  * Created by pc3 on 06.10.15.
  */
object BusinessCatalystOAuth {

  case class ExchangeTokenResponseAdditionalInfoSite(
                                                      site_id:Int,
                                                      secure_url:String,
                                                      name:Option[String]
                                                    )

  case class ExchangeTokenResponseAdditionalInfo(
                                                  secure_urls: Seq[String],
                                                  partnerId:Option[Int],
                                                  sites: Seq[ExchangeTokenResponseAdditionalInfoSite]
                                                )

  case class ExchangeTokenResponse(
                                    access_token:String,
                                    refresh_token:String,
                                    token_type:String,
                                    additional_info:ExchangeTokenResponseAdditionalInfo,
                                    expires_in: Int,
                                    refresh_token_expires_in: Int
                                  )

  case class SiteItemLink(
                           rel:String,
                           uri:String
                         )

  case class SiteItem(
                       id:Int,
                       name: String,
                       links: Seq[SiteItemLink],
                       siteLinks: Seq[SiteItemLink],
                       //  useAdvancedModuleDataEdit:String,
                       siteStatus:Int,
                       billingMethodTypeId:Int,
                       createDate:Date
                     )

  object SiteStatusCodes{
    val Unknown	 = 0x00
    val TrialInProgress =	0x01
    val TrialExpired = 0x02
    val GracePeriod	= 0x04
    val Deleted	= 0x08
    val Disabled	= 0x10
    val Paid =	0x20
    val Free	= 0x40
  }

  case class CustomField(
    objectId:Int,
    objectType:Int,
    value:JsValue
  )

  case class CustomFieldLabel(
     id:Int,
     label:String
  )

  case class LeadSourceType(
     id: Int,
     label:String
  )

  case class CustomerAddress(
    addressType: Int,
    objectId: Int,
    objectType: Int,
    address1: Option[String],
    address2: Option[String],
    zipCode: Option[String],
    suburb: Option[String],
    countryCode: Option[String],
    city:Option[String],
    state: Option[String],
    default: Option[Boolean]
  )

  case class Customer(
     id : Int,
     siteId: Int,
     leadSourceType: Option[LeadSourceType],
     industryType: Option[CustomFieldLabel],
     ratingType: Option[CustomFieldLabel],
     titleType: Option[CustomFieldLabel],
     email1: Option[CustomField],
     email2: Option[CustomField],
     email3: Option[CustomField],
     firstName: Option[String],
     middleName: Option[String],
     lastName: Option[String],
     company: Option[String],
     dateOfBirth: Option[Date],
     username: Option[String],
     workPhone:Option[CustomField],
     workFax:Option[CustomField],
     webAddress:Option[CustomField],
     customerType: Option[ CustomFieldLabel ],
     wholesaler: Boolean
  )

  case class BcCrmCustomerInfo(
    customer: BusinessCatalystOAuth.Customer,
    addresses: Seq[BusinessCatalystOAuth.CustomerAddress] = Seq()
  )


  implicit val exchangeTokenResponseAdditionalInfoSiteJsonFormat = Json.format[ExchangeTokenResponseAdditionalInfoSite]
  implicit val exchangeTokenResponseAdditionalInfoJsonFormat = Json.format[ExchangeTokenResponseAdditionalInfo]
  implicit val exchangeTokenResponseJsonFormat = Json.format[ExchangeTokenResponse]
  implicit val siteItemLinkJsonFormat = Json.format[SiteItemLink]
  implicit val siteItemJsonFormat = Json.format[SiteItem]

  implicit val leadSourceTypeToJson = Json.format[LeadSourceType]
  implicit val customFieldToJson = Json.format[CustomField]
  implicit val customFieldLabelToJson = Json.format[CustomFieldLabel]
  implicit val bcCrmCustomerToJson = Json.format[Customer]
  implicit val bcCrmCustomerAddressToJson = Json.format[CustomerAddress]


}
