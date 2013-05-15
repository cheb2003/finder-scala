package my.finder.common.util

import java.util.Properties

/**
 *
 */
object Config {
  val p:Properties = new Properties()
  def init = {
      val in = this.getClass.getClassLoader.getResourceAsStream("conf.properties")
      p.load(in)
      in.close()
  }
  def get(key:String) = {
    p.getProperty(key)
  }
}


