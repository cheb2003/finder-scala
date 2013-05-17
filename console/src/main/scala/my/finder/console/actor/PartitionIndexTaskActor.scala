package my.finder.console.actor

import akka.actor._
import my.finder.common.message._
import my.finder.index.actor.IndexDDProductActor
import akka.remote.RemoteScope
import akka.routing.FromConfig
import com.mongodb.casbah.Imports._
import my.finder.common.util.{Config, Util, Constants}
import java.text.SimpleDateFormat
import java.util.Date
import my.finder.common.message.CompleteIndexTask
import my.finder.common.message.ManageSubTask
import my.finder.common.message.PartitionIndexTaskMessage
import my.finder.common.message.IndexTaskMessage
import my.finder.common.message.CompleteSubTask
import my.finder.common.message.PartitionIndexTaskMessage
import my.finder.common.message.IndexTaskMessage
import java.lang
import my.finder.common.message.CompleteSubTask
import my.finder.common.message.MergeIndexMessage
import my.finder.common.message.IndexTaskMessage
import my.finder.common.message.PartitionIndexTaskMessage

/**
 *
 *
 */
class PartitionIndexTaskActor extends Actor with ActorLogging{
  val mongodbIP = Config.get("mongodbIP")
  val mongodbUser = Config.get("mongodbUser")
  val mongodbPassword = Config.get("mongodbPassword")
  val mongodbPort = lang.Integer.valueOf(Config.get("mongodbPort"))
  val uri = new MongoClientURI("mongodb://"+mongodbUser+":"+mongodbPassword+"@"+mongodbIP+":"+mongodbPort+"/?authMechanism=MONGODB-CR")
  val mongoClient =  MongoClient(uri)
  val dinobuydb = Config.get("dinobuydb")
  val ddProductIndexSize:Int = Integer.valueOf(Config.get("ddProductIndexSize"))
  val productColl = mongoClient(dinobuydb)("ec_productinformation")
  //TODO 改回来 var q:DBObject = ("ec_productprice.unitprice_money" $gt 0) ++ ("ec_product.isstopsale_bit" -> false)
  var q:DBObject = MongoDBObject.empty
  val fields = MongoDBObject("productid_int" -> 1)
  //val indexActor = context.system.actorOf(Props[IndexDDProductActor].withRouter(FromConfig()),"node")
  val indexActor = context.actorFor("akka://index@127.0.0.1:2554/user/root")
  val indexManager = context.actorFor("akka://console@127.0.0.1:2552/user/root/indexManager")
  //val mergeIndex = context.actorOf(Props[MergeIndexActor],"mergeIndex")




  override def preStart() {
    mongoClient.setReadPreference(ReadPreference.SecondaryPreferred)
  }

  def receive = {
    //分发子任务
    case msg:PartitionIndexTaskMessage => {
      if(msg.name == Constants.DD_PRODUCT){
        partitionDDProduct()
      }
    }

  }
  private def sendMsg(name:String,runId:String,seq:Int,set:Set[Int],total:Long) {
    indexActor ! IndexTaskMessage(Constants.DD_PRODUCT, runId,seq,set)
    indexManager ! CreateSubTask(name,runId,total)

  }
  def partitionDDProduct() = {
    val sdf:SimpleDateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss")
    val nowStr = sdf.format(new Date())
    var set:Set[Int] = Set()
    var i = 0
    var j = 0
    val totalCount:Long = productColl.count()
    //TODO
    val total:Long = totalCount / ddProductIndexSize + 1

    for(x <- productColl.find(q,fields,0,1000)){

      set += x.as[Int]("productid_int")
      i += 1
      if(i > ddProductIndexSize){
        i = 0
        j+=1
        sendMsg(Constants.DD_PRODUCT,nowStr,j,set,total)
        set = Set()
      }
    }

    if (i > 0) {
      j+=1
      sendMsg(Constants.DD_PRODUCT,nowStr,j,set,total)
    }
  }
}
