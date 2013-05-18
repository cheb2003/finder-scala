import sbt._
import Keys._

object HelloBuild extends Build {
    lazy val finder = Project(id = "finder",
                            base = file(".")) aggregate(console, index)

    val common = Project(id = "common",
                           base = file("common"))

    lazy val console = Project(id = "console",
                           base = file("console")) dependsOn(common,index)

    lazy val index = Project(id = "index",
                           base = file("index")) dependsOn(common)
}