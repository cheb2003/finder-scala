package my.finder.console.actor

import akka.actor.{ActorLogging, Actor}
import my.finder.common.util.{Constants, Config}
import java.lang
import com.mongodb.MongoClientURI
import com.mongodb.casbah.Imports._
import my.finder.console.service.{IndexManage, MongoManager}
import my.finder.common.message.{CreateSubTask, IndexTaskMessage, PartitionIndexTaskMessage, IndexIncremetionalTaskMessage}
import java.util.Date
import scala.collection.mutable.ListBuffer

/**
 *
 */
class PartitionUnitActor extends Actor with ActorLogging{
  val mongodbIP = Config.get("mongodbIP")
  val mongodbUser = Config.get("mongodbUser")
  val mongodbPassword = Config.get("mongodbPassword")
  val mongodbPort = lang.Integer.valueOf(Config.get("mongodbPort"))
  val uri = new MongoClientURI("mongodb://" + mongodbUser + ":" + mongodbPassword + "@" + mongodbIP + ":" + mongodbPort + "/?authMechanism=MONGODB-CR")
  var mongoClient:MongoClient = null//MongoClient(uri)
  val dinobuydb = Config.get("dinobuydb")
  val ddProductIndexSize: Int = Integer.valueOf(Config.get("ddProductIndexSize"))
  var productColl:MongoCollection = null//mongoClient(dinobuydb)("ec_productinformation")
  //TODO 改回来 var q:DBObject = ("ec_productprice.unitprice_money" $gt 0) ++ ("ec_product.isstopsale_bit" -> false)
  var q:DBObject = MongoDBObject.empty
  val fields = MongoDBObject("productid_int" -> 1)
  //val indexActor = context.system.actorOf(Props[IndexDDProductActor].withRouter(FromConfig()),"node")
  val indexRootActor = context.actorFor("akka://index@127.0.0.1:2554/user/root")
  //val unit = context.actorOf(Props[PartitionIndexTaskUnitActor].withRouter(RoundRobinRouter(nrOfInstances = 10)))
  val indexRootManager = context.actorFor("akka://console@127.0.0.1:2552/user/root/indexManager")
  //val mergeIndex = context.actorOf(Props[MergeIndexActor],"mergeIndex")


  override def preStart() {
    /*//val s = new ServerAddress("172.16.20.9",27017)
    val s = new ServerAddress("10.50.60.145",27018)
    val s1 = new ServerAddress("10.8.32.210",27018)
    val s2 = new ServerAddress("10.40.101.8",27018)


    val l = List[ServerAddress](s,s1,s2)
    //val l = List[ServerAddress](s)
    //val mc = MongoCredential("admin","admin","admin".toCharArray)
    val mc = MongoCredential("dinobuy","admin","DV42D6356K".toCharArray)
    //val mc1 = MongoCredential("admin","admin","admin".toCharArray)
    val ll = List[MongoCredential](mc)

    mongoClient = MongoClient(l,ll,MongoClientOptions.Defaults)
    mongoClient.setReadPreference(ReadPreference.SecondaryPreferred)*/
    mongoClient = MongoManager()
    productColl = mongoClient(dinobuydb)("ec_productinformation")
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

    val set:ListBuffer[Int] = new ListBuffer[Int]
    var time1 = System.currentTimeMillis();
    var time2 = System.currentTimeMillis();
    var id = 0
    for (y <- 1L to total) {
      time1 = System.currentTimeMillis()
      val items = productColl.find("productid_int" $gt id,fields,0,ddProductIndexSize).sort(MongoDBObject("productid_int" -> 1)).limit(ddProductIndexSize)
      for(x <- items){
        //set += x.as[Int]("productid_int")
        id = x.as[Int]("productid_int")
      }
      time2 = System.currentTimeMillis()
      items.close()
      log.info("spent in reading ids {}",time2 - time1)
      //sendMsg(Constants.DD_PRODUCT,now,y,set,total)
      set.remove(0,set.length)
    }


    /*if (i > 0) {
      j+=1
      sendMsg(Constants.DD_PRODUCT,now,j,set,total)
    }*/

  }
}
