package models

/*
* Step4
* Tuna這個資產的相關屬性
*
* */
case class Tuna(key:Option[String]=None,vessel:String,timestamp:String,location:String,holder:String)

case class Record(vessel:String,timestamp:String,location:String,holder:String)

case class TunaFromChainCode(key:Option[String]=None, Record:Record)

