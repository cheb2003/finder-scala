package my.finder.console.service

import my.finder.common.util.{Constants, Config}
import java.io.File
import scala.collection.mutable.ListBuffer

/**
 *
 */
case class Index(name:String,ids:ListBuffer[String],var using:String)

object IndexManage {

  var indexManage = ListBuffer[Index]()
  val ddIndex:Index = Index(Constants.DD_PRODUCT,ListBuffer[String](),"")
  def add(index:Index) = {
    indexManage += index
  }
  def get(indexName:String) = {
    /*for (i <- indexManage if i.name == indexName) i
    Index("",null,"")*/
    ddIndex
  }
  def init = {
    def findIndex(name:String) = {
      /*for (i <- indexManage) {
        if(i.name == name) i
      }
      Index(name,ListBuffer[String](),"")*/
      ddIndex
    }
    val dir = Config.get("workDir")

    //val files: util.Collection[File] = FileUtils.listFilesAndDirs(file,null,new PrefixFileFilter(Constants.DD_PRODUCT))
    var max:String = ""
    val files = new File(dir).listFiles()
    for(f:File <- files if f.isDirectory && !f.getName.endsWith("inc")) {
      //val s = f.getName().split("_")(0)
      val i:Index = findIndex(Constants.DD_PRODUCT)
      i.ids += f.getName
      if (max < f.getName) {
        max = f.getName
        i.using = max.split("_")(1)
      }
    }
    println(ddIndex)
  }
}
