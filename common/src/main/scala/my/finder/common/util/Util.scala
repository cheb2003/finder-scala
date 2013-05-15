package my.finder.common.util

/**
 *
 */
object Util {
  def getKey(name: String, runId: String) = {
    name + "_" + runId
  }
  def getPrefixPath(workDir:String,key:String) = {
    workDir + "/" + key + "/"
  }
}
