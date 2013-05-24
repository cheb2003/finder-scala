package my.finder.index.service

import org.apache.lucene.index.{IndexWriterConfig, IndexWriter}
import org.apache.lucene.store.{FSDirectory, Directory}
import java.io.File

import org.apache.lucene.util.Version
import my.finder.common.util.{Config, Util}
import akka.actor.Actor
import my.finder.common.message.{MergeIndexMessage, CloseIndexWriterMessage}
import my.finder.index.Analyzer.MyAnalyzer

/**
 *
 */
object IndexWriteManager{



  private var writerMap = Map[String, IndexWriter]()
  val workDir = Config.get("workDir")

  def getIndexWriter(name: String, runId: String): IndexWriter = {
    synchronized {

      val prefix = Util.getPrefixPath(workDir,Util.getKey(name,runId))

      val key = Util.getKey(name, runId)
      var writer: IndexWriter = writerMap getOrElse (key,null)
      if (writer == null) {
        val directory = FSDirectory.open(new File(prefix))
        val analyzer = new MyAnalyzer();
        val iwc = new IndexWriterConfig(Version.LUCENE_40, analyzer)
        iwc.setRAMBufferSizeMB(128)
        iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND)
        writer = new IndexWriter(directory,iwc)
        writerMap += (key -> writer)
      }
      writer
    }
  }
  def getIncIndexWriter(name: String, runId: String): IndexWriter = {
    synchronized {

      val prefix = Util.getPrefixPath(workDir,Util.getIncrementalPath(name,runId))

      val key = Util.getIncrementalPath(name, runId)
      var writer: IndexWriter = writerMap getOrElse (key,null)
      if (writer == null) {
        val directory = FSDirectory.open(new File(prefix))
        val analyzer = new MyAnalyzer();
        val iwc = new IndexWriterConfig(Version.LUCENE_40, analyzer)
        iwc.setRAMBufferSizeMB(128)
        iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND)
        writer = new IndexWriter(directory,iwc)
        writerMap += (key -> writer)
      }
      writer
    }
  }

}
class IndexWriteManager extends Actor{
  def receive = {
    case msg:CloseIndexWriterMessage => {
      val writer = IndexWriteManager.getIndexWriter(msg.name,msg.runId)
      writer.forceMerge(1)
      writer.close(true)
      val console = context.actorFor("akka://console@127.0.0.1:2552/user/root")
      val incPath = Util.getIncrementalPath(msg.name,msg.runId)
      val workDir = Config.get("workDir")
      val file = new File(workDir + "/" + incPath)
      val timeFile = new File(workDir + "/" + incPath + "/time")
      if(!file.exists()){
        file.mkdir();
      }
      timeFile.createNewFile()
      console ! MergeIndexMessage(msg.name,msg.runId)
    }
  }
}
