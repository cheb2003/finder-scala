package my.finder.console.actor

import akka.actor.{Props, Actor}
import my.finder.common.message._
import akka.routing.RandomRouter
import my.finder.common.util.Constants

/**
 *
 *
 */
class ConsoleRootActor extends Actor {
  val partitionActor = context.actorOf(Props[PartitionIndexTaskActor], "partitiontor")
  val indexManagerActor = context.actorOf(Props[IndexManagerActor], "indexManager")
  val mergeIndex = context.actorOf(Props[MergeIndexActor],"mergeIndex")
  def receive = {
    case msg: CommandParseMessage => {
      if (msg.command == Constants.DD_PRODUCT) {
        partitionActor ! PartitionIndexTaskMessage(Constants.DD_PRODUCT)
        //println("index----" + Thread.currentThread().getName + " " + this)

      }
      if (msg.command == "search") {
        val search = context.actorFor("akka://SearchApp@127.0.0.1:2555/user/root")
        search ! ChangeIndexMessage("DD_PRODUCT")
      }
    }
    case msg:MergeIndexMessage => {

    }
  }
}
