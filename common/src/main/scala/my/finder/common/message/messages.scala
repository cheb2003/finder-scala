package my.finder.common.message

import java.util.Date
import scala.collection.mutable.ListBuffer

case class IndexTaskMessage(name:String,date:Date,seq:Long,ids:ListBuffer[Int]);
case class IndexIncremetionalTaskMessage(name:String,date:Date);
case class PartitionIndexTaskMessage(name:String)
case class CommandParseMessage(command:String)
case class CompleteSubTask(name:String,date:Date,seq:Long,successCount:Int,failCount:Int,skipCount:Int)
case class CreateSubTask(name:String,date:Date,total:Long)
case class ManageSubTask(name:String,date:Date)
case class CompleteIndexTask(name:String,date:Date)
case class CompleteIncIndexTask(name:String,date:Date,successCount:Int,failCount:Int,skipCount:Int)
case class CloseWriter(name:String,date:Date)
case class MergeIndexMessage(name:String,date:Date)
case class ChangeIndexMessage(name:String,date:Date)
case class CloseIndexWriterMessage(name:String,date:Date)
case class GetIndexesPathMessage()
case class IncIndexeMessage(name:String,date:Date)
case class GetIndexesPathMessageReponse(msg:List[String])

