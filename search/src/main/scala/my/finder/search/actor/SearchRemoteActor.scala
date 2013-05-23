package my.finder.search.actor

import akka.actor.{Props, Actor}
import my.finder.common.message.{ChangeIndexMessage, GetIndexesPathMessage}
import my.finder.search.service.{ServiceLocator, SearcherManager}

//import my.finder.common.message.{GetIndexesPathMessage, ChangeIndexMessage}

/**
 *
 */
abstract class SearchRemoteActor extends Actor{

  /*def receive = {
    case msg:ChangeIndexMessage => {
      val sm = ServiceLocator.getService[SearcherManager](Class[SearcherManager]);
      sm.changeSearcher(msg.name,msg.id)
    }
    /*case GetIndexesPathMessage => {
      val consoleRoot = context.actorFor("akka://console@127.0.0.1:2552/user/root")
      consoleRoot ! GetIndexesPathMessage
    }*/
  }*/

}
