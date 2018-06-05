package controllers


import javax.inject._
import play.api.mvc._
import play.api.libs.json._
import play.api.Logger

import models.AppUser

import java.nio.file.Files
import java.nio.file.Paths


import utils.UtilTools

import controllers.custom.actions.LoggingAction


/*
*
*
*
* */
@Singleton
class FabricController @Inject()(
                                  loggingAction: LoggingAction,
                                  cc: ControllerComponents) extends AbstractController(cc)  {


  private val logger = Logger(this.getClass)


  /*
  *
  * Step7
  * 用戶提出申請，檢查Json是否合法
  *   若不合法，則回傳BadRequest
  *   若合法，則進行管理員校驗，檢查用戶是否已存在
  *     若不存在，則創建，並回傳成功消息告知
  *     若存在，則回傳Forbidden，告知用戶已創建
  *
  * */
  def registerAdmin =  Action(parse.json){ implicit  request =>

    val appUserJsonMappingResult:Option[AppUser] = AppUser.apply(request.body)

    appUserJsonMappingResult.fold(
      ifEmpty = BadRequest("Your request is a invalid request")
    )(
      appUser => {
        logger.info(appUser.toString)
        UtilTools.tryDeserialize(appUser.name).fold(
          ifEmpty = {
//          透過原本CA預設的帳號密碼製作出一個管理員
            val adminEnrollment = UtilTools.hfCaClient.enroll("admin", "adminpw")
            UtilTools.serialize(appUser.copy(enrollment = adminEnrollment))
            Ok("用戶已創建")
          }
        )(
          _ => Forbidden("用戶身份已經存在")
        )
      }
    )
  }


  /*
  *
  * Step8
  * 對方丟post，抓出裡面的name，
  *   檢查本地端是否具有此key
  *     若有，則在cookie上附加
  *     若無，則傳回Unauthorized。
  *
  * */
  def loginByPem = Action(parse.json){ implicit request =>
    request.body.\("name").toOption.fold(
      ifEmpty = BadRequest("封包內容有誤")
    )( userName =>

      if(Files.exists(Paths.get(userName.as[String] + ".jso"))){
        Ok("登錄成功").withCookies(Cookie(name="X-Authorization",value=userName.as[String]))
      }else{
        Unauthorized("沒有此用戶")
      }

    )
  }

/*
*
* Step10
* 提出註冊一般用戶，檢查c現在用戶是否為管理員，檢測本地檔案是否具有該用戶
*   若不合法，則回傳Unauthorized，告知無效管理員身份
*   若合法，則檢查用戶是否已存在，
*     若不存在，則使用管理員身份進行創建，並回傳成功消息告知
*     若存在，則回傳Forbidden，告知用戶已創建
*
* */
  def registerUser = loggingAction(parse.json){ implicit request =>
    request.body.\("name").toOption.fold(
      ifEmpty= BadRequest("未填寫用戶id")
    )( userName =>
      UtilTools.tryDeserialize(userName.as[String]) match{
        case None =>{
          val rr = new org.hyperledger.fabric_ca.sdk.RegistrationRequest(userName.as[String], "org1")
          val registrar = UtilTools.tryDeserialize(request.cookies.get("X-Authorization").get.value).get
          val enrollmentSecret = UtilTools.hfCaClient.register(rr, registrar)
          val userEnrollment = UtilTools.hfCaClient.enroll(userName.as[String], enrollmentSecret)
          val appUser = AppUser(userName.as[String], affiliation="org1", mspId ="Org1MSP", enrollment = userEnrollment)
          UtilTools.serialize(appUser)
          Ok("用戶已成功註冊")
        }
        case Some(_) =>{
          Forbidden("用戶已存在")
        }
      }
    )
  }

}