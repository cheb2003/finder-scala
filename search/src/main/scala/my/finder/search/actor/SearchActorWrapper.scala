package my.finder.search.actor

import akka.actor.{Props, ActorSystem}
import com.typesafe.config.ConfigFactory


/**
 *
 */
object SearchActorWrapper {
  val system = ActorSystem.create("search",ConfigFactory.parseResources(this.getClass.getClassLoader,"application.conf").getConfig("search"))
  def init() = {
    system.actorOf(Props[SearchRootActor], "root")
  }
  def destroy() = {
    system.shutdown()
  }
}
