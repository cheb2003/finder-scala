package my.finder.index.actor

import akka.actor.{ActorLogging, Actor}
import my.finder.common.util.{MongoUtil, Config}
import my.finder.index.service.MongoManager._
import com.mongodb.casbah.Imports._
import org.apache.lucene.document._

import my.finder.index.service.IndexWriteManager
import my.finder.common.message.{CompleteSubTask, IndexTaskMessage}
import org.apache.commons.lang.StringUtils
import java.util.Date

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
    , "ec_product.businessname_nvarchar" -> 1
  )
  private val pIdField = new IntField("pId", 0, Field.Store.YES);
  private val pNameField = new TextField("pName", "", Field.Store.YES)
  private val priceField = new DoubleField("unitPrice", 0.0f, Field.Store.YES)
  private val indexCodeField = new StringField("indexCode", "", Field.Store.YES)
  private val isOneSaleField = new IntField("isOneSale", 0, Field.Store.YES)
  private val isAliExpressField = new IntField("isAliExpress", 0, Field.Store.YES)
  private val skuField = new StringField("sku", "", Field.Store.YES)
  private val businessNameField = new StringField("businessName", "", Field.Store.YES)
  private val pNameRuField = new TextField("pNameRU", "", Field.Store.YES)
  private val createTimeField = new StringField("createTime", "", Field.Store.YES)
  private var doc: Document = null

  override def preStart() {
    mongoClient.setReadPreference(ReadPreference.SecondaryPreferred)
  }

  def receive = {
    case msg: IndexTaskMessage => {
      val writer = IndexWriteManager.getIndexWriter(msg.name, msg.runId)
      var list: MongoDBList = null
      var successCount = 0
      var failCount = 0
      for (x <- productColl.find("productid_int" $in msg.ids, fields)) {
        try {
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


          list = x.as[MongoDBList]("ec_productprice")

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
          }
          doc.add(pIdField)
          doc.add(pNameField)
          doc.add(indexCodeField)
          doc.add(skuField)
          writer.addDocument(doc)
          successCount += 1
        } catch {
          case e: Exception => log.error(e, "index item fail,productId {}",x.as[Int]("productid_int"));failCount += 1
        }
      }
      val indexManager = context.actorFor("akka://console@127.0.0.1:2552/user/root/indexManager")
      indexManager ! CompleteSubTask(msg.name,msg.runId,msg.seq,successCount,failCount)
    }

  }
}
