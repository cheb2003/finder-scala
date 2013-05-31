package my.finder.console.service

import my.finder.common.util.Config
import java.lang
import com.mongodb.MongoClientURI
import com.mongodb.casbah.Imports._

/**
 *
 */
object MongoManager {
  val mongodbIP = Config.get("mongodbIP")
  val mongodbUser = Config.get("mongodbUser")
  val mongodbPassword = Config.get("mongodbPassword")
  val mongodbPort = lang.Integer.valueOf(Config.get("mongodbPort"))
  //val uri = new MongoClientURI("mongodb://"+mongodbUser+":"+mongodbPassword+"@"+mongodbIP+":"+mongodbPort+"/?authMechanism=MONGODB-CR")
  var mongoClient:MongoClient = null//MongoClient(new MongoClientURI("mongodb://"+mongodbUser+":"+mongodbPassword+"@"+mongodbIP+":27018/?authMechanism=MONGODB-CR"))
  def apply():MongoClient = {
    if (mongoClient == null) {
      val s = new ServerAddress("172.16.20.9",27017)
      /*val s = new ServerAddress("10.50.60.145",27018)
      val s1 = new ServerAddress("10.8.32.210",27018)
      val s2 = new ServerAddress("10.40.101.8",27018)*/


      //val l = List[ServerAddress](s,s1,s2)
      val l = List[ServerAddress](s)
      val mc = MongoCredential("admin","admin","admin".toCharArray)
      //val mc = MongoCredential("dinobuy","admin","DV42D6356K".toCharArray)
      //val mc1 = MongoCredential("admin","admin","admin".toCharArray)
      val ll = List[MongoCredential](mc)

      mongoClient = MongoClient(l,ll,MongoClientOptions.Defaults)
      mongoClient.setReadPreference(ReadPreference.SecondaryPreferred)
    }
    mongoClient
  }
}
