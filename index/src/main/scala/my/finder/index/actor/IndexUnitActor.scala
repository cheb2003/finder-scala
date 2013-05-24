package my.finder.index.actor

import akka.actor.{ActorLogging, Actor}
import my.finder.common.util.{Util, MongoUtil, Config}
import my.finder.index.service.MongoManager._
import com.mongodb.casbah.Imports._
import org.apache.lucene.document._

import my.finder.index.service.IndexWriteManager
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


  val productColl = mongoClient(dinobuydb)("ec_productinformation")


  val fields = MongoDBObject("productid_int" -> 1, "ec_product.productaliasname_nvarchar" -> 1
    , "ec_productprice.unitprice_money" -> 1, "ec_product.productbrand_nvarchar" -> 1
    , "ec_product.businessbrand_nvarchar" -> 1, "ec_product.indexcode_nvarchar" -> 1
    , "ec_product.productbrandid_int" -> 1, "ec_product.isonesale_tinyint" -> 1
    , "ec_product.isaliexpress_tinyint" -> 1, "productkeyid_nvarchar" -> 1
    , "ec_productlanguage" -> 1, "ec_product.createtime_datetime" -> 1
    , "ec_product.businessname_nvarchar" -> 1, "ec_product.isstopsale_bit" -> 1
    , "ec_product.qdwproductstatus_int" -> 1
  )
  val sort = MongoDBObject("productid_int" -> 1)

  private val pIdField = new IntField("pId", 0, Field.Store.YES);
  private val pNameField = new TextField("pName", "", Field.Store.YES)
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
    mongoClient.setReadPreference(ReadPreference.SecondaryPreferred)
  }
  def writeDoc(x: DBObject, writer: IndexWriter) {
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
        //successCount += 1
      } else {
        //skipCount += 1
      }
    } catch {
      case e: Exception => log.error("index item fail,productId {};", x.as[Int]("productid_int")); //failCount += 1
    }
  }
  def receive = {
    case msg:IndexIncremetionalTaskMessage => {
      val time1 = System.currentTimeMillis();

      val incPath = Util.getIncrementalPath(msg.name,msg.runId)
      val timeFile = new File(workDir + "/" + incPath + "/time")
      val date = new Date(timeFile.lastModified())
      val q = "ec_product.createtime_datetime" $gt date
      val writer = IndexWriteManager.getIncIndexWriter(msg.name, msg.runId)
      for (x <- productColl.find(q, fields).sort(sort)) {
        writeDoc(x, writer)
      }
      writer.commit();
      timeFile.delete()
      timeFile.createNewFile()
      val time2 = System.currentTimeMillis();
      log.info("index incremental spent {}",time2 - time1)
      val consoleRoot = context.actorFor("akka://console@127.0.0.1:2552/user/root")
      consoleRoot ! CompleteIncIndexTask(msg.name, msg.runId)
    }
    case msg: IndexTaskMessage => {
      val writer = IndexWriteManager.getIndexWriter(msg.name, msg.runId)

      var successCount = 0
      var failCount = 0
      var skipCount = 0
      for (x <- productColl.find(MongoDBObject.empty, fields).sort(sort).skip(Integer.valueOf((msg.seq * 2000).toString())).limit(2000)) {

        writeDoc(x, writer)


      }
      val indexManager = context.actorFor("akka://console@127.0.0.1:2552/user/root")
      indexManager ! CompleteSubTask(msg.name, msg.runId, msg.seq, successCount, failCount, skipCount)
    }

  }
}
