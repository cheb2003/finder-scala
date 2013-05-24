package my.finder.common.util

/**
 *
 */
object Util {
  def getKey(name: String, runId: String) = {
    name + "_" + runId
  }
  def getIncrementalPath(name: String, runId: String) = {
    name + "_" + runId + "_inc"
  }
  def getPrefixPath(workDir:String,key:String) = {
    workDir + "/" + key + "/"
  }
}
