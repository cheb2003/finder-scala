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
  val productColl = mongoClient(dinobuydb)("EC_ProductInformation")
  //var q:DBObject = ("ec_product.productprice_money" $gt 0) ++ ("ec_product.isstopsale_bit" -> false)
  var q:DBObject = MongoDBObject.empty
  val fields = MongoDBObject("productid_int" -> 1)
  val indexActor = context.system.actorOf(Props[IndexDDProductActor].withRouter(FromConfig()),"node")
  val mergeIndex = context.actorOf(Props[MergeIndexActor],"mergeIndex")

  var subTaskMap = Map[String,Map[String,Int]]()


  override def preStart() {
    mongoClient.setReadPreference(ReadPreference.SecondaryPreferred)

  }

  def receive = {
    //分发子任务
    case msg:PartitionIndexTaskMessage => {
      if(msg.name == Constants.DD_PRODUCT){
        log.info("send subtask ddproduct")
        partitionDDProduct()
      }
    }
    //统计子任务完成数，完成后合并索引
    case msg:CompleteSubTask => {
      val key = Util.getKey(msg.name, msg.runId)
      var obj:Map[String,Int] = subTaskMap.getOrElse(key,null)
      val i = obj("completed") + 1
      obj += ("completed" -> i)
      subTaskMap += (key -> obj)
      log.info("完成子任务 {},{},{}。当前完成数{}/{}", Array(msg.name,msg.runId,msg.seq,i,subTaskMap(key)("total")))
      if(subTaskMap(key)("completed") >= subTaskMap(key)("total")){
        mergeIndex ! MergeIndexMessage(msg.name,msg.runId,subTaskMap(key)("total"))
      }
    }
  }
  private def sendMsg(name:String,runId:String,seq:Int,set:Set[Int]) {
    indexActor ! IndexTaskMessage(Constants.DD_PRODUCT, runId,seq,set)
    //管理索引子任务
    val key = Util.getKey(name, runId)
    var obj: Map[String, Int] = subTaskMap.getOrElse(key, null)
    if (obj == null) {
      //新建
      var map = Map[String,Int]()
      map += ("completed" -> 0)
      map += ("total" -> 1)
      subTaskMap += (key -> map)
      log.info("create index {},task {}", key,subTaskMap(key)("total"))
    } else {
      //累计
      val i = obj("total") + 1
      obj += ("total" -> i)
      subTaskMap += (key -> obj)
      //log.info("add sub task {} to {}", key,subTaskMap(key)("total"))
    }
  }
  def partitionDDProduct() = {
    val sdf:SimpleDateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss")
    val nowStr = sdf.format(new Date())
    var set:Set[Int] = Set()
    var i = 0
    var j = 0

    for(x <- productColl.find(q,fields,0,1000)){

      set += x.as[Int]("productid_int")
      i += 1
      if(i > ddProductIndexSize){
        i = 0
        j+=1
        sendMsg(Constants.DD_PRODUCT,nowStr,j,set)
        set = Set()
      }
    }

    if (i > 0) {
      j+=1
      sendMsg(Constants.DD_PRODUCT,nowStr,j,set)
    }
  }
}
