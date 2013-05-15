name := "hello"

version := "1.0"

scalaVersion := "2.10.1"

libraryDependencies ++= Seq(
    "commons-lang" % "commons-lang" % "2.6" withSources()
)

resolvers += "Local Maven Repository" at "file://D:/快盘/dev/localRepository/"