import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._

enablePlugins(ScalaJSPlugin)

name := "crosswordmaker"

version := "0.2.0-SNAPSHOT"

scalaVersion := "3.2.0"

libraryDependencies ++= Seq(
  "org.scala-js" %%% "scalajs-dom" % "2.1.0",
  "com.lihaoyi" %%% "upickle" % "2.0.0"
)

Compile / fastOptJS / crossTarget := file("docs/js")
