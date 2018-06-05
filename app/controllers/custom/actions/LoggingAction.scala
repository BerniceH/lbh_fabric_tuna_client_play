package controllers.custom.actions

import java.nio.file.{Files, Paths}

import akka.util.ByteString
import com.google.rpc.BadRequest
import javax.inject.Inject
import play.api.mvc._

import scala.concurrent.ExecutionContext

import scala.concurrent.Future
import play.api.Logger
import play.api.http.HttpEntity

/*
*
* Step9
* 檢查cookie特定欄位 X-Authorization
*   若無，則回傳401
*   若有，則稽核是否有此檔案
*     若無，則回傳Forbidden
*     若有，則通過。
*
* */
class LoggingAction @Inject() (parser: BodyParsers.Default)(implicit ec: ExecutionContext) extends ActionBuilderImpl(parser) {
  override def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[Result]) = {
    Logger.info("Calling action")
    request.cookies.get("X-Authorization").fold(
      ifEmpty = Future(Result(ResponseHeader(401),HttpEntity.Strict(ByteString("驗證方式錯誤"),Some("text/plain"))))
    )(  auth =>
      if(Files.exists(Paths.get(auth.value + ".jso"))){
        block(request)
      }else{
        Future(Result(ResponseHeader(401),HttpEntity.Strict(ByteString("無此用戶"),Some("text/plain"))))
      }
    )
  }
}