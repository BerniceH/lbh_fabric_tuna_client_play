package utils

import models.AppUser

import java.io.{ObjectInputStream, ObjectOutputStream}
import java.nio.file.{Files, Paths}

import scala.util.Try

import org.hyperledger.fabric_ca.sdk.HFCAClient
import org.hyperledger.fabric.sdk.HFClient
import org.hyperledger.fabric.sdk.security.CryptoSuite

/*
*
* Step6
* 設定與hyperledger連線的客戶端
* CA Server 客戶端
* Chain 客戶端
*
* 把AppUser物件序列化成一本地檔案
* 將本地檔案反序列化成AppUser物件
*
* */
object UtilTools {


  val hfCaClient = {
    val cryptoSuite: org.hyperledger.fabric.sdk.security.CryptoSuite  = org.hyperledger.fabric.sdk.security.CryptoSuite.Factory.getCryptoSuite

    //    創建客戶端
    val caClient = HFCAClient.createNewInstance("http://localhost:7054", null)

    //    設置該客戶端的加密套件
    caClient.setCryptoSuite(cryptoSuite)
    caClient

  }

  /**
    *
    * 啟動一個 HFClient
    *
    *
    * */
  def hfClient: HFClient = { // initialize default cryptosuite
    val cryptoSuite = CryptoSuite.Factory.getCryptoSuite
    // setup the client
    val client = HFClient.createNewInstance
    client.setCryptoSuite(cryptoSuite)
    client
  }

  /**
    * 將用戶物件作序列化之後，存成檔案
    *
    * */
  def serialize(appUser: AppUser): Unit = {
    Try{
      val oos = new ObjectOutputStream(Files.newOutputStream(Paths.get(appUser.getName + ".jso")))
      oos.writeObject(appUser)
      oos.close()
    }
  }

  /**
    * 讀取檔案後，將檔案內容轉成物件，而後進行操作
    *
    * */
  def tryDeserialize(name: String): Option[AppUser] = {
    if (Files.exists(Paths.get(name + ".jso"))){
      val decoder = new ObjectInputStream(Files.newInputStream(Paths.get(name + ".jso")))

      val tryAppUser = Try{
        decoder.readObject.asInstanceOf[AppUser]
      }
      tryAppUser.foreach( _ => decoder.close() )
      tryAppUser.toOption
    }else{
      None
    }
  }


}
