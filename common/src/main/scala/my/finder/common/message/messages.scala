package my.finder.common.message
case class IndexTaskMessage(name:String,runId:String,seq:Long);
case class PartitionIndexTaskMessage(name:String)
case class CommandParseMessage(command:String)
case class CompleteSubTask(name:String,runId:String,seq:Long,successCount:Int,failCount:Int,skipCount:Int)
case class CreateSubTask(name:String,runId:String,total:Long)
case class ManageSubTask(name:String,runId:String)
case class CompleteIndexTask(name:String,runId:String)
case class CloseWriter(name:String,runId:String)
case class MergeIndexMessage(name:String,runId:String)
case class ChangeIndexMessage(name:String,id:String)
case class CloseIndexWriterMessage(name:String,runId:String)
case class GetIndexesPathMessage()
case class GetIndexesPathMessageReponse(msg:List[String])

