import Dependencies._

name := """rainbow"""
organization := "edu.uci.ics.cloudberry"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).
  settings(
    resolvers += "smile-core".at("https://mvnrepository.com/artifact/com.github.haifengl/smile-core")
  ).
  settings(
    libraryDependencies ++= appDependencies
  ).
  settings(
    mappings in Universal ++=
      (baseDirectory.value / "public" / "data" * "*" get) map
        (x => x -> ("public/data/" + x.getName))
  ).enablePlugins(PlayJava)

scalaVersion := "2.13.0"

libraryDependencies += guice

val osName: SettingKey[String] = SettingKey[String]("osName")

osName := (System.getProperty("os.name") match {
    case name if name.startsWith("Linux") => "linux"
    case name if name.startsWith("Mac") => "mac"
    case name if name.startsWith("Windows") => "win"
    case _ => throw new Exception("Unknown platform!")
})

libraryDependencies += "org.openjfx" % "javafx-base" % "11-ea+25" classifier osName.value

libraryDependencies += "org.openjfx" % "javafx-controls" % "11-ea+25" classifier osName.value

libraryDependencies += "org.openjfx" % "javafx-fxml" % "11-ea+25" classifier osName.value

libraryDependencies += "org.openjfx" % "javafx-graphics" % "11-ea+25" classifier osName.value

PlayKeys.devSettings += "play.server.http.idleTimeout" -> "3600 seconds"
