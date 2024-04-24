enablePlugins(ScalaJSPlugin)

name := "crossword-puzzle-maker"

version := "0.1.0-SNAPSHOT"

scalaVersion := "2.12.16"

libraryDependencies ++= Seq(
  "org.scala-js" %%% "scalajs-dom" % "2.1.0",
  "com.lihaoyi" %%% "upickle" % "2.0.0"
)

Compile / fastOptJS / scalaJSLinkerConfig ~= {
  _.withSourceMap(false) // Desabilita a geração de source maps
}
