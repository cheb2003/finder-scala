package my.finder.index.actor

import akka.actor.{ActorLogging, Actor}
import my.finder.index.service.IndexManager
import com.mongodb.casbah.Imports._
import my.finder.common.message.{CompleteSubTask, IndexTaskMessage}
import org.apache.lucene.document._
import scala.Boolean
import my.finder.common.util.{MongoUtil, Config, Util}
import org.apache.lucene.index.{IndexWriterConfig, IndexWriter}
import org.apache.lucene.store.FSDirectory
import java.io.File
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.util.Version
import my.finder.common.message.CompleteSubTask
import my.finder.common.message.IndexTaskMessage
import java.util.Date
import java.lang
import org.apache.commons.lang.StringUtils

/**
 *
 *
 */
class IndexDDProductActor extends Actor with ActorLogging with MongoUtil{
  val workDir = Config.get("workDir")
  val mongodbIP = Config.get("mongodbIP")
  val dinobuydb = Config.get("dinobuydb")
  val mongodbUser = Config.get("mongodbUser")
  val mongodbPassword = Config.get("mongodbPassword")
  val mongodbPort = lang.Integer.valueOf(Config.get("mongodbPort"))
  val uri = new MongoClientURI("mongodb://"+mongodbUser+":"+mongodbPassword+"@"+mongodbIP+":"+mongodbPort+"/?authMechanism=MONGODB-CR")
  val mongoClient =  MongoClient(uri)

  val productColl = mongoClient(dinobuydb)("EC_ProductInformation")//EC_ProductInformation
  val q:DBObject = MongoDBObject.empty
  //TODO 价格要变，店铺分类
  val fields = MongoDBObject("productid_int" -> 1,"ec_product.productaliasname_nvarchar" -> 1
    ,"ec_product.productprice_money" -> 1,"ec_product.productbrand_nvarchar" -> 1
    ,"ec_product.businessbrand_nvarchar" -> 1,"ec_product.indexcode_nvarchar" -> 1
    ,"ec_product.productbrandid_int" -> 1,"ec_product.isonesale_tinyint" -> 1
    ,"ec_product.isaliexpress_tinyint" -> 1,"productkeyid_nvarchar" -> 1
    ,"ec_productlanguage" -> 1
    )
  //"ec_product.productscore_float" -> 1 "ec_product.createtime_DateTime" -> 1
  //"ec_product.discount_int" -> 1

  private val pIdField = new IntField("pId",0,Field.Store.YES);
  private val pNameField = new TextField("pName","",Field.Store.YES)
  private val priceField = new DoubleField("productprice_money",0.0f,Field.Store.YES)
  private val indexCodeField = new StringField("indexcode_nvarchar","",Field.Store.YES)
  private val isOneSaleField = new IntField("isOneSale",0,Field.Store.YES)
  private val isAliExpressField = new IntField("isAliExpress",0,Field.Store.YES)
  private val skuField = new StringField("sku","",Field.Store.YES)
  private val pNameRuField = new TextField("pNameRu","",Field.Store.YES)
  private var doc:Document = null


  override def preStart() {
    //mongoClient.setReadPreference(ReadPreference.SecondaryPreferred)
  }

  def receive = {
    case msg:IndexTaskMessage => {
      println("IndexTaskMessage")
      //val writer = IndexManager.getIndexWriter(msg.name,msg.runId)
      //var writer: IndexWriter = writerMap getOrElse (key,null)
      val key = Util.getKey(msg.name, msg.runId)
      val prefix = Util.getPrefixPath(workDir,key)
      val directory = FSDirectory.open(new File(prefix + msg.seq))
      val analyzer = new StandardAnalyzer(Version.LUCENE_43);
      val iwc = new IndexWriterConfig(Version.LUCENE_43, analyzer)
      iwc.setRAMBufferSizeMB(128)
      iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND)
      val writer = new IndexWriter(directory,iwc)
      var langs:MongoDBList = null
      var k = 0;
      //val ite = productColl.find("productid_int" $in msg.ids,fields)
      for(x <- productColl.find("productid_int" $in msg.ids,fields)){
        try{
          k += 1
          log.info("{}",k)
          pIdField.setIntValue(x.as[Int]("productid_int"));
          pNameField.setStringValue(StringUtils.defaultIfBlank(mvp[String](x,"productaliasname_nvarchar"),StringUtils.defaultIfBlank(mvp[String](x,"businessbrand_nvarchar"),"")) + ' ' + mvp[String](x,"productaliasname_nvarchar"))
          indexCodeField.setStringValue(mvp[String](x,"indexcode_nvarchar"))
          priceField.setDoubleValue(mvp[Double](x,"productprice_money"))
          try{
            isOneSaleField.setIntValue(mvp[Int](x,"isonesale_tinyint"))
            isAliExpressField.setIntValue(mvp[Int](x,"isaliexpress_tinyint"))
          } catch {
            case e:Exception =>
          }


          skuField.setStringValue(mv[String](x,"productkeyid_nvarchar"))
          langs = x.as[MongoDBList]("ec_productlanguage")
          for (y <- 0 until langs.length) {
            if(langs.as[DBObject](y).as[String]("language_nvarchar") == "ru"){
              pNameRuField.setStringValue(langs.as[DBObject](y).as[String]("producttitle_nvarchar"))
            }
          }
          doc = new Document()
          doc.add(pIdField)
          doc.add(pNameField)
          doc.add(priceField)
          doc.add(indexCodeField)
          doc.add(isAliExpressField)
          doc.add(isOneSaleField)
          doc.add(pNameRuField)
          doc.add(skuField)
          writer.addDocument(doc)
        } catch {
          case e:Exception => log.error(e,"取芒果数据错误")
        }
      }
      writer.close()
      sender ! CompleteSubTask(msg.name,msg.runId,msg.seq)
    }
  }
}
