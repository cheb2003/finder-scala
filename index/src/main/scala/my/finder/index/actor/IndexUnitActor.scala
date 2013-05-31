package my.finder.index.actor

import akka.actor.{ActorLogging, Actor}
import my.finder.common.util.{Util, MongoUtil, Config}
import my.finder.index.service.MongoManager.mongoClient
import com.mongodb.casbah.Imports._
import org.apache.lucene.document._

import my.finder.index.service.{MongoManager, IndexWriteManager}
import my.finder.common.message.{CompleteIncIndexTask, IndexIncremetionalTaskMessage, CompleteSubTask, IndexTaskMessage}
import org.apache.commons.lang.StringUtils
import java.util.Date
import org.apache.lucene.index.IndexWriter
import java.io.File

/**
 *
 */
class IndexUnitActor extends Actor with ActorLogging with MongoUtil {
  val workDir = Config.get("workDir")
  val dinobuydb = Config.get("dinobuydb")


  var productColl:MongoCollection = null


  val fields = MongoDBObject("productid_int" -> 1, "ec_product.productaliasname_nvarchar" -> 1
    , "ec_productprice.unitprice_money" -> 1, "ec_product.productbrand_nvarchar" -> 1
    , "ec_product.businessbrand_nvarchar" -> 1, "ec_product.indexcode_nvarchar" -> 1
    , "ec_product.productbrandid_int" -> 1, "ec_product.isonesale_tinyint" -> 1
    , "ec_product.isaliexpress_tinyint" -> 1, "productkeyid_nvarchar" -> 1
    , "ec_productlanguage" -> 1, "ec_product.createtime_datetime" -> 1
    , "ec_product.businessname_nvarchar" -> 1, "ec_product.isstopsale_bit" -> 1
    , "ec_product.qdwproductstatus_int" -> 1
  )
  //val sort = MongoDBObject("productid_int" -> 1)

  private val pIdField = new IntField("pId", 0, Field.Store.YES);
  private val pNameField = new TextField("pName", "aa", Field.Store.YES)
  private val priceField = new DoubleField("unitPrice", 0.0f, Field.Store.YES)
  private val indexCodeField = new StringField("indexCode", "", Field.Store.YES)
  private val isOneSaleField = new IntField("isOneSale", 0, Field.Store.YES)
  private val isAliExpressField = new IntField("isAliExpress", 0, Field.Store.YES)
  private val skuField = new StringField("sku", "", Field.Store.YES)
  private val businessNameField = new StringField("businessName", "", Field.Store.YES)
  private val pNameRuField = new TextField("pNameRU", "", Field.Store.YES)
  private val pNameBrField = new TextField("pNameBR", "", Field.Store.YES)
  private val createTimeField = new StringField("createTime", "", Field.Store.YES)
  private var doc: Document = null

  override def preStart() {
    val mongo = MongoManager()
    productColl = mongo(dinobuydb)("ec_productinformation")
  }
  def writeDoc(x: DBObject, writer: IndexWriter):Boolean = {
    var list: MongoDBList = null
    try {
      if (mvp[Int](x, "qdwproductstatus_int") < 2 && mvp[Boolean](x, "isstopsale_bit") == false
        && x.as[MongoDBList]("ec_productprice").length > 0 && x.as[MongoDBList]("ec_productprice").as[DBObject](0).as[Double]("unitprice_money") > 0) {
        list = x.as[MongoDBList]("ec_productprice")
        doc = new Document()
        pIdField.setIntValue(x.as[Int]("productid_int"));
        pNameField.setStringValue(StringUtils.defaultIfBlank(mvp[String](x, "productaliasname_nvarchar"), StringUtils.defaultIfBlank(mvp[String](x, "businessbrand_nvarchar"), "")) + ' ' + mvp[String](x, "productaliasname_nvarchar"))
        indexCodeField.setStringValue(mvp[String](x, "indexcode_nvarchar"))
        try {
          createTimeField.setStringValue(DateTools.dateToString(mvp[Date](x, "createtime_datetime"), DateTools.Resolution.MINUTE))
          doc.add(createTimeField)
        } catch {
          case e: Exception =>
        }
        if (mvp[String](x, "businessname_nvarchar").trim != "") {
          businessNameField.setStringValue(mvp[String](x, "businessname_nvarchar"))
          doc.add(businessNameField)
        }
        try {
          priceField.setDoubleValue(list.as[DBObject](0).as[Double]("unitprice_money"))
          doc.add(priceField)
        } catch {
          case e: Exception =>
        }
        try {
          isOneSaleField.setIntValue(mvp[Int](x, "isonesale_tinyint"))
          doc.add(isOneSaleField)
        } catch {
          case e: Exception =>
        }
        try {
          isAliExpressField.setIntValue(mvp[Int](x, "isaliexpress_tinyint"))
          doc.add(isAliExpressField)
        } catch {
          case e: Exception =>
        }
        skuField.setStringValue(mv[String](x, "productkeyid_nvarchar"))
        list = x.as[MongoDBList]("ec_productlanguage")
        for (y <- 0 until list.length) {
          if (list.as[DBObject](y).as[String]("language_nvarchar").toLowerCase() == "ru") {
            pNameRuField.setStringValue(list.as[DBObject](y).as[String]("producttitle_nvarchar"))
            doc.add(pNameRuField)
          }
          if (list.as[DBObject](y).as[String]("language_nvarchar").toLowerCase() == "br") {
            pNameBrField.setStringValue(list.as[DBObject](y).as[String]("producttitle_nvarchar"))
            doc.add(pNameBrField)
          }
        }
        doc.add(pIdField)
        doc.add(pNameField)
        doc.add(indexCodeField)
        doc.add(skuField)
        writer.addDocument(doc)
        true
        //successCount += 1
      } else {
        //skipCount += 1
        false
      }
    } catch {
      case e: Exception => log.error("index item fail,productId {};", x.as[Int]("productid_int")); throw e//failCount += 1
    }

    /*doc = new Document
    doc.add(pIdField)
    doc.add(pNameField)
    doc.add(indexCodeField)
    doc.add(skuField)
    writer.addDocument(doc)*/
  }
  def receive = {
    case msg:IndexIncremetionalTaskMessage => {
      val time1 = System.currentTimeMillis();

      val incPath = Util.getIncrementalPath(msg.name,msg.date)
      val timeFile = new File(workDir + "/" + incPath + "/time")
      val from = new Date(timeFile.lastModified())
      val to = new Date
      var successCount = 0
      var failCount = 0
      var skipCount = 0
      val q = "ec_product.createtime_datetime" $gte from $lt to
      val writer = IndexWriteManager.getIncIndexWriter(msg.name, msg.date)

      val items: MongoCursor = productColl.find(q, fields)
      for (x <- items) {

        try{
          if(writeDoc(x, writer)) successCount += 1 else skipCount += 1
        } catch {
          case e:Exception => failCount += 1
        }

      }
      /*for (x <- 1 to 100) {
        writeDoc(null, writer)
      }*/
      writer.commit();
      //TODO 应该时间排序取最后一个记录的时间，作为lastupdatetime
      timeFile.delete()
      timeFile.createNewFile()
      timeFile.setLastModified(to.getTime)
      val time2 = System.currentTimeMillis();
      log.info("index incremental spent {}",time2 - time1)
      val consoleRoot = context.actorFor("akka://console@127.0.0.1:2552/user/root")
      log.info("index incremental {}/{}",successCount,items.size)
      consoleRoot ! CompleteIncIndexTask(msg.name, msg.date,successCount,failCount,skipCount)
    }
    case msg: IndexTaskMessage => {
      //log.info("recevie indextaskmessage {}",msg.date)
      val time1 = System.currentTimeMillis()
      val writer = IndexWriteManager.getIndexWriter(msg.name, msg.date)
      //val now:Date = msg.date
      var successCount:Int = 0
      var failCount:Int = 0
      var skipCount:Int = 0
      //var time3 = System.currentTimeMillis()
      //val items: MongoCursor = productColl.find("ec_product.createtime_datetime" $lt now, fields).skip(Integer.valueOf((msg.seq * 2).toString())).limit(2000)
      val items: MongoCursor = productColl.find("productid_int" $in msg.ids, fields)
      //var time4 = System.currentTimeMillis()
      var b = false

      //log.info("find items {}",time4 - time3)


      //log.info("spent {} millisecond in finding items {}",time2 - time1,items.size)
      for (x <- items) {
        //if(!b) b = true else log.info("load item {}",time3 -time4)
        //time3 = System.currentTimeMillis()
        try{
          if(writeDoc(x, writer)) successCount += 1 else skipCount += 1
        } catch {
          case e:Exception => failCount += 1
        }
        //time4 = System.currentTimeMillis()
      }
      /*for (x <- 1 to 100) {
        writeDoc(null, writer)
      }*/
      val indexManager = context.actorFor("akka://console@127.0.0.1:2552/user/root")
      indexManager ! CompleteSubTask(msg.name, msg.date, msg.seq, successCount, failCount, skipCount)
      val time2 = System.currentTimeMillis()
      items.close()
      val arr = new Array[Int](5)
      arr(0) = Integer.valueOf((time2 - time1).toString)
      arr(1) = successCount
      arr(2) = failCount
      arr(3) = skipCount
      arr(4) = items.size
      log.info("index time {} success {} fail {} skip {} total {}",arr);
    }
  }
}
