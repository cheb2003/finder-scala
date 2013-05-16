package my.finder.search.actor

import akka.actor.{Props, ActorSystem}
import com.typesafe.config.ConfigFactory


/**
 *
 */
object ActorWrapper {
  val system = ActorSystem.create("SearchApp",ConfigFactory.parseResources(this.getClass.getClassLoader,"application.conf").getConfig("search"))
  def init = {
    system.actorOf(Props[SearchRemoteActor], "root")
    println("init SearchApp")
  }
  def destroy = {
    system.shutdown()
  }
}
