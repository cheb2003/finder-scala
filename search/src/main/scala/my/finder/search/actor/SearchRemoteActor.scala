package my.finder.search.actor

import akka.actor.{Props, Actor}
import my.finder.common.message.ChangeIndexMessage

/**
 *
 */
class SearchRemoteActor extends Actor{

  def receive = {
    case msg:ChangeIndexMessage => {
        println(msg.indexRunId)
    }
  }
}
