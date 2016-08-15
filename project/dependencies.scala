import sbt._

object Dependencies {

  object Version {
    val akka = "2.4.8"
  }

  lazy val frontend = common ++ tests
  lazy val backend = common ++ tests

  val common = Seq(
    "com.typesafe.akka" %% "akka-actor" % Version.akka,
    "com.typesafe.akka" %% "akka-testkit" % Version.akka,
    "com.typesafe.akka" %% "akka-http-experimental" % Version.akka,
    "com.typesafe.akka" %% "akka-http-spray-json-experimental" % Version.akka,
    "ch.qos.logback" %  "logback-classic" % "1.1.7",
    "com.typesafe.scala-logging" %% "scala-logging" % "3.4.0"
  )

  val tests = Seq(
    "org.scalatest" %% "scalatest" % "2.2.4" % "test",
    "junit" % "junit" % "4.12" % "test",
    "com.novocode" % "junit-interface" % "0.11" % "test",
    "org.scalatestplus" %% "play" % "1.4.0-M3" % "test",
    "com.typesafe.akka" %% "akka-testkit" % Version.akka % "test"
  )

}
