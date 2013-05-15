package my.finder.index

import akka.actor.{Props, ActorSystem}
import com.typesafe.config.ConfigFactory
import my.finder.index.actor.IndexDDProductActor
import my.finder.common.util.Config

/**
 * @author ${user.name}
 */
object IndexApp {
  

  
  def main(args : Array[String]) {
    Config.init
    val system = ActorSystem("nodeApp", ConfigFactory
      .load().getConfig("node"))
    /*system.actorOf(Props[IndexDDProductActor], name = "indexActor")*/
  }

}
