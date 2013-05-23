package my.finder.console.actor

import akka.actor.{Props, Actor}
import my.finder.common.message._
import akka.pattern.ask
import my.finder.common.util.Constants
import akka.util.Timeout

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.Future
import my.finder.console.service.{Index, IndexManage}

/**
 *
 *
 */
class ConsoleRootActor extends Actor {

  val partitionActor = context.actorOf(Props[PartitionIndexTaskActor], "partitiontor")
  val indexManagerActor = context.actorOf(Props[IndexManagerActor], "indexManager")
  val mergeIndex = context.actorOf(Props[MergeIndexActor],"mergeIndex")
  def receive = {
    case msg:GetIndexesPathMessage => {

    }
    case msg:CompleteSubTask => {
      indexManagerActor ! msg
    }
    case msg: CommandParseMessage => {
      if (msg.command == Constants.DD_PRODUCT) {
        partitionActor ! PartitionIndexTaskMessage(Constants.DD_PRODUCT)
      }
      if (msg.command == "changeIndex") {
        val search = context.actorFor("akka://SearchApp@127.0.0.1:2555/user/root")
        val i:Index = IndexManage.get(Constants.DD_PRODUCT)
        search ! ChangeIndexMessage(i.name,i.using)
      }
    }
    case msg:MergeIndexMessage => {

    }
  }
}
