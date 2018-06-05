package models



import java.util

import org.hyperledger.fabric.sdk.{Enrollment, User}
import play.api.Logger

import collection.JavaConverters._
import collection.mutable._
import scala.util.Try
import play.api.libs.json._


/*
*
* Step3 設定業務場景所需的用戶類別
* 必須繼承User，並實作其方法，另外可按業務場景新增屬性
*
* */
case class AppUser(
                    var name:String ,
                    var  roles:java.util.Set[String]=null ,
                    var account:String = null,
                    var  affiliation:String ="org11",
                    var  enrollment:Enrollment=null,
                    var mspId:String ="Org1MSP"
                  ) extends User with Serializable{

  private val serializationId = 1L

  override def getName: String = name

  override def getRoles: util.Set[String] = roles

  override def getAccount: String = account

  override def getAffiliation: String = affiliation

  override def getEnrollment: Enrollment = enrollment

  override def getMspId: String = mspId

  override def toString: String = "AppUser{" + "name='" + name + '\'' + "\n, roles=" + roles + "\n, account='" + account + '\'' + "\n, affiliation='" + affiliation + '\'' + "\n, enrollment=" + enrollment + "\n, mspId='" + mspId + '\'' + '}'

}

/*
*
* 自定義一個json轉換物件方法。
*
* */
object AppUser{

  private val logger = Logger(this.getClass)

  def apply(appUserJson:JsValue):Option[AppUser]={

    logger.info(appUserJson.\("name").get.as[String])
    Try{
      AppUser(
        name= appUserJson.\("name").get.as[String]
      )
    }.toOption
  }

}


