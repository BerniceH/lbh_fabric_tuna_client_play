package controllers

import java.util

import controllers.custom.actions.LoggingAction
import javax.inject._

import org.hyperledger.fabric.sdk.{ChaincodeID, HFClient}
import play.api.mvc._

import utils.UtilTools

import collection.JavaConverters._


import play.api.libs.json._

@Singleton
class TunaController @Inject()(
                                loggingAction: LoggingAction,
                                cc: ControllerComponents) extends AbstractController(cc)  {


  implicit val tunaFormat = Json.format[models.Tuna]

  /*
  *
  * 取得區塊鏈上的Channel
  * 進行重要節點設定
  *
  * */
  def getChannel(hfClient:HFClient) = { // initialize channel
    // peer name and endpoint in fabcar network
    val peer = hfClient.newPeer("peer0.org1.example.com", "grpc://localhost:7051")
    // eventhub name and endpoint in fabcar network
    val eventHub = hfClient.newEventHub("eventhub01", "grpc://localhost:7053")
    // orderer name and endpoint in fabcar network
    val orderer = hfClient.newOrderer("orderer.example.com", "grpc://localhost:7050")
    // channel name in fabcar network
    val channel = hfClient.newChannel("mychannel")
    channel.addPeer(peer)
    channel.addEventHub(eventHub)
    channel.addOrderer(orderer)
    channel.initialize
    channel
  }

  /*
  *
  * 調用查詢類型的Chaincode
  *
  * */
  def invokeQueryChaincode(
                       appUser:org.hyperledger.fabric.sdk.User,
                       chaincodeName:String="lbh-tuna-demo",
                       functionName:String,
                       args: Array[String]
                     ) = {
    val hfClient = utils.UtilTools.hfClient
    hfClient.setUserContext(appUser)
    val channel = getChannel(hfClient)
    val queryPRQ = hfClient.newQueryProposalRequest
    val chaincodeId = ChaincodeID.newBuilder.setName(chaincodeName).build
    queryPRQ.setChaincodeID(chaincodeId)
    queryPRQ.setFcn(functionName)
    queryPRQ.setArgs(new util.ArrayList[String](args.toBuffer.asJava))

    val res = channel.queryByChaincode(queryPRQ)
    res.asScala
  }

  /*
  *
  * 調用交易更新數據類型的chaincode
  *
  * */
  def invokeTransactionChaincode(
                            appUser:org.hyperledger.fabric.sdk.User,
                            chaincodeName:String="lbh-tuna-demo",
                            functionName:String,
                            args: Array[String]
                          ) = {
    val hfClient = utils.UtilTools.hfClient
    hfClient.setUserContext(appUser)
    val channel = getChannel(hfClient)
    val tranPRQ = hfClient.newTransactionProposalRequest()
    val chaincodeId = ChaincodeID.newBuilder.setName(chaincodeName).build
    tranPRQ.setChaincodeID(chaincodeId)
    tranPRQ.setFcn(functionName)
    tranPRQ.setArgs(new util.ArrayList[String](args.toBuffer.asJava))

    val res = channel.sendTransactionProposal(tranPRQ)
    val transcationConfirm = channel.sendTransaction(res)
    res.asScala
  }


  //Compose Action的應用場景
  /*
  *
  * Step11
  * 檢查用戶是否合法
  *   若不合法，則Unauthorized
  *   若合法，則使用QueryString進行查詢，
  *
  * output
  *   [{"holder":"Miriam","location":"67.0006, -70.5476","timestamp":"1504054225","vessel":"923F"}]
  * */
  def getTuna(key:String) = loggingAction{ implicit request =>
    val clientName = request.cookies.get("X-Authorization").get.value
    UtilTools.tryDeserialize(clientName).fold(
      ifEmpty = Unauthorized("無此用戶")
    )(appUser =>{

      val resultIter = invokeQueryChaincode(appUser,functionName = "queryTuna",args=Array(key))

      val resultJsonArray = resultIter.map{ record =>
        Json.parse(new String(record.getChaincodeActionResponsePayload))
      }.toArray

      Ok(Json.stringify(Json.toJson(resultJsonArray)))
    })
  }

  /*
  *
  * Step12
  * 檢查用戶是否合法
  *   若不合法，則Unauthorized
  *   若合法，則檢驗封包body,
  *     若不符合內容，則BadRequest退回
  *     若符合內容，則塞入資料
  *
  * */
  def addTuna = loggingAction(parse.json){ implicit request =>
    val clientName = request.cookies.get("X-Authorization").get.value
    UtilTools.tryDeserialize(clientName).fold(
      ifEmpty = Unauthorized("無此用戶")
    )(appUser =>{
      Json.fromJson[models.Tuna](request.body).fold(
        _ => BadRequest("封包內容不符合"),
        tuna => {
          val resultIter = invokeTransactionChaincode(appUser,functionName = "recordTuna",args=Array(tuna.key.get ,tuna.vessel,tuna.timestamp,tuna.location,tuna.holder))

          if(resultIter.head.getChaincodeActionResponseStatus==200){
            Ok("v_postTuna")
          }else{
            InternalServerError("請重新塞入資料")
          }
        }
      )
    })
  }

  /*
  *
  * Step13
  * 檢查用戶是否合法
  *   若不合法，則Unauthorized
  *   若合法，則檢驗封包body,
  *     若不符合內容，則BadRequest退回
  *     若符合內容，則將資料取出，進行持有者變更
  *

  * */
  def changeTunaOwner = loggingAction(parse.json){ implicit request =>
    val clientName = request.cookies.get("X-Authorization").get.value
    UtilTools.tryDeserialize(clientName).fold(
      ifEmpty = Unauthorized("無此用戶")
    )(appUser =>{
      Json.fromJson[models.Tuna](request.body).fold(
        _ => BadRequest("封包內容不符合"),
        tuna => {
          val resultIter = invokeTransactionChaincode(appUser,functionName = "changeTunaHolder",args=Array(tuna.key.get,tuna.holder))

          if(resultIter.head.getChaincodeActionResponseStatus==200){
            Ok("v_putTuna")
          }else{
            InternalServerError("請重新塞入資料")
          }
        }
      )
    })
  }

  /*
  *
  * Step14
  * 檢查用戶是否合法
  *   若不合法，則Unauthorized
  *   若合法，則查詢資料，並傳回
  *
  * output
  *   [{"holder":"Miriam","location":"67.0006, -70.5476","timestamp":"1504054225","vessel":"923F"},
  *    {"holder":"Miriam","location":"67.0006, -70.5476","timestamp":"1504054225","vessel":"923F"}]
  *
  * */
  def getAllTuna = loggingAction{ implicit request =>
    val clientName = request.cookies.get("X-Authorization").get.value
    UtilTools.tryDeserialize(clientName).fold(
      ifEmpty = Unauthorized("無此用戶")
    )(appUser =>{

      val resultIter = invokeQueryChaincode(appUser,functionName = "queryAllTuna",args=Array[String]())

      val resultJsonArray = resultIter.map{ record =>
        Json.parse(new String(record.getChaincodeActionResponsePayload))
      }.toArray

      Ok(Json.stringify(Json.toJson(resultJsonArray)))
    })
  }






}
