package my.finder.search.actor

import akka.actor.{Props, Actor}
import my.finder.common.message.{IncIndexeMessage, ChangeIndexMessage, GetIndexesPathMessage}
import my.finder.search.service.{ServiceLocator, SearcherManager}

//import my.finder.common.message.{GetIndexesPathMessage, ChangeIndexMessage}

/**
 *
 */
class SearchRootActor extends Actor{

  def receive = {
    case msg:ChangeIndexMessage => {
      val sm = ServiceLocator.getService("searcherManager").asInstanceOf[SearcherManager];
      sm.changeSearcher(msg.name,msg.id)
    }
    case msg:IncIndexeMessage => {
      val sm = ServiceLocator.getService("searcherManager").asInstanceOf[SearcherManager];
      sm.updateIncrementalIndex(msg.name,msg.id)
    }
    /*case GetIndexesPathMessage => {
      val consoleRoot = context.actorFor("akka://console@127.0.0.1:2552/user/root")
      consoleRoot ! GetIndexesPathMessage
    }*/
  }

}
