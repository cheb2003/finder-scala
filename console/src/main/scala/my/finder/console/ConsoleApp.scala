package my.finder.console
import akka.actor.{Props, ActorSystem}
import my.finder.console.actor.ConsoleRootActor
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
    val system = ActorSystem.create("console", ConfigFactory.load().getConfig("console"))
    val root = system.actorOf(Props[ConsoleRootActor], "root")

    val scanner = new Scanner(System.in)
    var command:String = null
    while(true){
      command = scanner.nextLine()
      if (command == "indexDDProduct") {
        root ! CommandParseMessage(Constants.DD_PRODUCT)
      }
      if (command == "search") {
        root ! CommandParseMessage("search")
      }
    }
  }
}
