import sbt._
import play.sbt.PlayImport._

object Dependencies {

  val appDependencies: Seq[ModuleID] = Seq(
    ws, // Play's web services module
    "org.webjars" % "bootstrap" % "3.3.6",
    "org.webjars" % "flot" % "0.8.0",
    "org.webjars" % "angularjs" % "1.5.0",
    "org.eclipse.jetty.websocket" % "websocket-client" % "9.4.7.v20170914",
    "org.webjars" % "leaflet" % "0.7.2",
    "org.webjars" % "angular-leaflet-directive" % "0.8.2",
    "org.webjars.bower" % "json-bigint" % "0.0.0",
    "org.webjars.bower" % "bootstrap-toggle" % "2.2.2",
    // jackson
    "com.fasterxml.jackson.core" % "jackson-core" % "2.9.4",
    // PostgreSQL
    "org.postgresql" % "postgresql" % "42.2.1",
    // Smile ML Toolkit
    "com.github.haifengl" % "smile-core" % "1.5.3"
  )
}
