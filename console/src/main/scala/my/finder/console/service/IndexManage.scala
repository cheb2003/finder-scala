package my.finder.console.service

import java.util.{Collections, Date}
import my.finder.common.util.{Constants, Config}
import java.io.File
import org.apache.commons.io.FileUtils
import org.apache.commons.io.filefilter.PrefixFileFilter
import java.util
import scala.collection.mutable.ListBuffer

/**
 *
 */
case class Index(name:String,ids:ListBuffer[String],var using:String)

object IndexManage {

  var indexManage = ListBuffer[Index]()
  def add(index:Index) = {
    indexManage += index
  }
  def get(indexName:String) = {
    for (i <- indexManage if i.name == indexName) i
    Index("",null,"")
  }
  def init = {
    def findIndex(name:String) = {
      for (i <- indexManage) {
        if(i.name == name) i
      }
      Index(name,ListBuffer[String](),"")
    }
    val dir = Config.get("workDir")
    val file = new File(dir)
    //val files: util.Collection[File] = FileUtils.listFilesAndDirs(file,null,new PrefixFileFilter(Constants.DD_PRODUCT))
    var max:String = ""
    val files = new File(dir).listFiles()
    for(f:File <- files if f.isDirectory) {
      //val s = f.getName().split("_")(0)
      val i:Index = findIndex(Constants.DD_PRODUCT)
      i.ids += f.getName
      if (max < f.getName) {
        max = f.getName
        i.using = max
      }
    }
  }
}
