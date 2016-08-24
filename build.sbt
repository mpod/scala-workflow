val commonSettings = Seq(
  version := "1.0",
  scalaVersion := "2.11.8"
)

lazy val root = (project in file("."))
    .settings(
      name := "scala-workflow",
      version := "1.0"
    )
    .aggregate(common, backend, frontend)

lazy val common = (project in file("common"))
    .settings(
      name := "api-common",
      libraryDependencies ++= Dependencies.common,
      commonSettings
    )

lazy val backend = (project in file("backend"))
  .settings(
    name := "akka-backend",
    libraryDependencies ++= Dependencies.backend,
    commonSettings
  ).dependsOn(common)

lazy val frontend = (project in file("frontend"))
  .enablePlugins(PlayScala)
  .settings(
    name := "play-frontend",
    libraryDependencies ++= Dependencies.frontend,
    routesGenerator := InjectedRoutesGenerator,
    commonSettings
  ).dependsOn(common)
