package my.finder.common.message
case class IndexTaskMessage(name:String,runId:String,seq:Int,ids:Set[Int]);
case class PartitionIndexTaskMessage(name:String)
case class CommandParseMessage(command:String)
case class CompleteSubTask(name:String,runId:String,seq:Int)
case class ManageSubTask(name:String,runId:String)
case class CompleteIndexTask(name:String,runId:String)
case class CloseWriter(name:String,runId:String)
case class MergeIndexMessage(name:String,runId:String,total:Int)