package my.finder.index.service

import my.finder.common.util.Config
import java.lang
import com.mongodb.casbah.Imports._

/**
 *
 */
object MongoManager {
  val mongodbIP = Config.get("mongodbIP")
  val mongodbUser = Config.get("mongodbUser")
  val mongodbPassword = Config.get("mongodbPassword")
  val mongodbPort = lang.Integer.valueOf(Config.get("mongodbPort"))
  val uri = new MongoClientURI("mongodb://"+mongodbUser+":"+mongodbPassword+"@"+mongodbIP+":"+mongodbPort+"/?authMechanism=MONGODB-CR")
  val mongoClient =  MongoClient(uri)
}
