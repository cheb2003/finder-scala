name := "finder"

version := "1.0"

scalaVersion in ThisBuild := "2.10.1"

libraryDependencies in ThisBuild ++= Seq(
    "commons-lang" % "commons-lang" % "2.6" withSources() withJavadoc(),
    "org.mongodb" % "casbah_2.10" % "2.6.0",
    "com.typesafe.akka" % "akka-remote_2.10" % "2.1.4" withSources(),
    "com.typesafe.akka" % "akka-actor_2.10" % "2.1.4" withSources(),
    "com.typesafe.akka" % "akka-slf4j_2.10" % "2.1.4" withSources(),
    "com.typesafe" % "config" % "1.0.0",
    "com.typesafe.akka" % "akka-kernel_2.10" % "2.1.4" withSources(),
    "org.apache.lucene" % "lucene-analyzers-common" % "4.0.0",
    "org.apache.lucene" % "lucene-core" % "4.0.0"
)

transitiveClassifiers := Seq("sources")



