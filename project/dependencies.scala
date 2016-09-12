import sbt._

object Dependencies {

  object Version {
    val akka = "2.4.9"
  }

  lazy val frontend = common ++ webjars ++ tests
  lazy val backend = common ++ tests

  val common = Seq(
    "com.typesafe.akka" %% "akka-actor" % Version.akka,
    "com.typesafe.akka" %% "akka-remote" % Version.akka,
    "com.typesafe.akka" %% "akka-testkit" % Version.akka,
    "com.typesafe.akka" %% "akka-http-experimental" % Version.akka,
    "com.typesafe.akka" %% "akka-http-spray-json-experimental" % Version.akka,
    "ch.qos.logback" %  "logback-classic" % "1.1.7",
    "com.typesafe.scala-logging" %% "scala-logging" % "3.4.0"
  )

  val webjars = Seq(
    "org.webjars" %% "webjars-play" % "2.5.0",
    "org.webjars" % "jquery" % "3.1.0",
    "org.webjars" % "font-awesome" % "4.6.3",
    "org.webjars" % "bootstrap" % "3.3.7-1" exclude ("org.webjars", "jquery")
  )

  val tests = Seq(
    "org.scalatest" %% "scalatest" % "3.0.0" % "test",
    "junit" % "junit" % "4.12" % "test",
    "com.typesafe.akka" %% "akka-testkit" % Version.akka % "test"
  )

}
