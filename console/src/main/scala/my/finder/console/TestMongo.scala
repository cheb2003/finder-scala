package my.finder.console

import com.mongodb.casbah.Imports._
import com.mongodb.util.JSON
import scala.io.Source
import my.finder.common.util.Config

/**
 *
 */
object TestMongo {
  def main(args: Array[String]) {
    /*Config.init
    println(Config.get("workDir"))*/
    val mongoClient =  MongoClient("172.16.20.9", 27017)
    var productColl = mongoClient("dinobuydb")("ec_productinformation")
    //var q = ("ec_product" -> MongoDBObject("isstopsale_bit" -> false))
    var q:DBObject = ("ec_product.productprice_money" $gt 0) ++ ("ec_product.isstopsale_bit" -> false)
    //var q:DBObject = MongoDBObject("ec_product.isstopsale_bit" -> false)
    val fields = MongoDBObject("productid_int" -> 1,"ec_product.productaliasname_nvarchar" -> 1
      ,"ec_product.createtime_DateTime" -> 1,"ec_product.discountprice_money" -> 1
      ,"ec_product.productscore_float" -> 1)
    //val sort = MongoDBObject("productid_int" -> -1)
    //,"ec_product.producttypeid_int" -> 1
    /*for(x <- Source.fromFile("f:/product.json").getLines()){

      productColl += JSON.parse(x).asInstanceOf[DBObject]
    }*/
    /*val max = productColl.find(q,fields).limit(1).sort(sort).next
    var lst:Set[Int] = Set()*/
    /*for(x <- productColl.find(q,fields,0,1)){
      lst += x.as[Int]("productid_int")
    }
    println(lst)

    for(x <- productColl.find("productid_int" $in lst,fields).skip(2).limit(2).sort(sort)){
      println(x)
    }*/
    /*def getValue[B](a1: => DBObject, a2: => B){

    }*/
    /*for(x <- productColl.find("ec_product.productprice_money" $gt 0)){
      //println(x.as[DBObject]("ec_product").as[String]("productaliasname_nvarchar"))
      println(x)
    }*/
    println(productColl.count(q))
    /*for(x <- productColl.find(q)){
      println(x.as[DBObject]("ec_product").as[String]("productaliasname_nvarchar"))
    }*/

    //productColl.insert()
  }
}
