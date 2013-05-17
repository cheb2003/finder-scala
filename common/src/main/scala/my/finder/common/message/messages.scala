package my.finder.common.message
case class IndexTaskMessage(name:String,runId:String,seq:Int,ids:Set[Int]);
case class PartitionIndexTaskMessage(name:String)
case class CommandParseMessage(command:String)
case class CompleteSubTask(name:String,runId:String,seq:Int,successCount:Int,failCount:Int)
case class CreateSubTask(name:String,runId:String,total:Long)
case class ManageSubTask(name:String,runId:String)
case class CompleteIndexTask(name:String,runId:String)
case class CloseWriter(name:String,runId:String)
case class MergeIndexMessage(name:String,runId:String)
case class ChangeIndexMessage(indexRunId:String)
case class CloseIndexWriterMessage(name:String,runId:String)
