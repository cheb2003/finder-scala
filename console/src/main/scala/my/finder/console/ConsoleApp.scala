package my.finder.console
import akka.actor.{Props, ActorSystem}
import my.finder.console.actor.RootActor
import my.finder.common.message.{CommandParseMessage, PartitionIndexTaskMessage, IndexTaskMessage}
import com.typesafe.config.ConfigFactory
import java.util.Scanner
import my.finder.common.util.{Config, Constants}

/**
 * @author ${user.name}
 */
object ConsoleApp {
  def main(args : Array[String]) {
    Config.init("console.properties")
    val system = ActorSystem.create("ConsoleApp", ConfigFactory.load().getConfig("console"))
    val index = system.actorOf(Props[RootActor], "root")

    /*0 to 100 foreach{
      i => index ! CommandParseMessage("ddProduct")
    }*/
    val scanner = new Scanner(System.in)
    var command:String = null//= scanner.nextLine()
    while(true){
      command = scanner.nextLine()
      if (command == "indexDDProduct") {
        println("send command indexDDProduct")
        index ! CommandParseMessage(Constants.DD_PRODUCT)
      }
    }
  }
}
