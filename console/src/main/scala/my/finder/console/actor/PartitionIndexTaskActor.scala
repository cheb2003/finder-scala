package my.finder.console.actor

import akka.actor._
import my.finder.common.message._

import akka.routing.{RoundRobinRouter, FromConfig}
import com.mongodb.casbah.Imports._
import my.finder.common.util.{Config, Util, Constants}

import java.util.Date

import java.lang

import my.finder.common.message.IndexTaskMessage
import my.finder.common.message.PartitionIndexTaskMessage
import my.finder.console.service.IndexManage
import scala.collection.mutable.ListBuffer

/**
 *
 *
 */
class PartitionIndexTaskActor extends Actor with ActorLogging {
  val mongodbIP = Config.get("mongodbIP")
  val mongodbUser = Config.get("mongodbUser")
  val mongodbPassword = Config.get("mongodbPassword")
  val mongodbPort = lang.Integer.valueOf(Config.get("mongodbPort"))
  val uri = new MongoClientURI("mongodb://" + mongodbUser + ":" + mongodbPassword + "@" + mongodbIP + ":" + mongodbPort + "/?authMechanism=MONGODB-CR")
  val mongoClient = MongoClient(uri)
  val dinobuydb = Config.get("dinobuydb")
  val ddProductIndexSize: Int = Integer.valueOf(Config.get("ddProductIndexSize"))
  val productColl = mongoClient(dinobuydb)("ec_productinformation")
  //TODO 改回来 var q:DBObject = ("ec_productprice.unitprice_money" $gt 0) ++ ("ec_product.isstopsale_bit" -> false)
  var q:DBObject = MongoDBObject.empty
  val fields = MongoDBObject("productid_int" -> 1)
  //val indexActor = context.system.actorOf(Props[IndexDDProductActor].withRouter(FromConfig()),"node")
  val indexRootActor = context.actorFor("akka://index@127.0.0.1:2554/user/root")
  //val unit = context.actorOf(Props[PartitionIndexTaskUnitActor].withRouter(RoundRobinRouter(nrOfInstances = 10)))
  val indexRootManager = context.actorFor("akka://console@127.0.0.1:2552/user/root/indexManager")
  //val mergeIndex = context.actorOf(Props[MergeIndexActor],"mergeIndex")


  override def preStart() {
    mongoClient.setReadPreference(ReadPreference.SecondaryPreferred)
  }

  def receive = {
    case msg: IndexIncremetionalTaskMessage => {
      val i = IndexManage.get(Constants.DD_PRODUCT)
      indexRootActor ! IndexIncremetionalTaskMessage(i.name, i.using)
    }
    //分发子任务
    case msg: PartitionIndexTaskMessage => {
      if (msg.name == Constants.DD_PRODUCT) {
        partitionDDProduct()
      }
    }
  }
  var ii = 0
  private def sendMsg(name: String, runId: Date, seq: Long,ids:ListBuffer[Int], total: Long) {
    ii += 1
    indexRootActor ! IndexTaskMessage(Constants.DD_PRODUCT, runId, seq,ids)
    indexRootManager ! CreateSubTask(name, runId, total)
  }

  def partitionDDProduct() = {

    val now = new Date()

    //val totalCount: Long = 100024L
    log.info("create index {}",now)

    //val totalCount: Long = productColl.count("ec_product.createtime_datetime" $lt now)
    val totalCount: Long = productColl.count()

    //log.info("spent {} millisecond in querying total items {}",time2 - time1,totalCount)
    //TODO
    val total: Long = totalCount / ddProductIndexSize + 1
    /*for (x <- 0L until total) {
      sendMsg(Constants.DD_PRODUCT, now, x, total)
    }*/
    var i = 0
    var j = 0
    val set:ListBuffer[Int] = new ListBuffer[Int]
    var time1 = System.currentTimeMillis();
    var time2 = System.currentTimeMillis();
    val items = productColl.m
    for(x <- items){

      set += x.as[Int]("productid_int")
      i += 1
      if(i >= ddProductIndexSize){
        i = 0
        j+=1
        sendMsg(Constants.DD_PRODUCT,now,j,set,total)
        time2 = System.currentTimeMillis()
        log.info("spent in reading ids {}",time2 - time1)
        time1 = time2
        set.remove(0,set.length)
        //log.info("set length {}",set.length)
      }
    }

    if (i > 0) {
      j+=1
      sendMsg(Constants.DD_PRODUCT,now,j,set,total)
    }
    println("------------------------" + ii)
  }
}
