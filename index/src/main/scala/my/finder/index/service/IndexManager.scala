package my.finder.index.service

import org.apache.lucene.index.{IndexWriterConfig, IndexWriter}
import org.apache.lucene.store.{FSDirectory, Directory}
import java.io.File
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.util.Version
import my.finder.common.util.Util

/**
 *
 */
object IndexManager {
  private val lock = null
  private var writerMap = Map[String, IndexWriter]()

  def getIndexWriter(name: String, runId: String): IndexWriter = {


    synchronized {
      val key = Util.getKey(name, runId)
      var writer: IndexWriter = writerMap getOrElse (key,null)
      if (writer == null) {
        val directory = FSDirectory.open(new File(key))
        val analyzer = new StandardAnalyzer(Version.LUCENE_40);
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
