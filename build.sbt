val commonSettings = Seq(
  version := "1.0",
  scalaVersion := "2.11.8"
)

lazy val root = (project in file("."))
    .settings(
      name := "scala-workflow",
      version := "1.0"
    )
    .aggregate(backend, frontend)

lazy val backend = (project in file("backend"))
  .settings(
    name := "akka-backend",
    libraryDependencies ++= Dependencies.backend,
    commonSettings
  )

lazy val frontend = (project in file("frontend"))
  .enablePlugins(PlayScala)
  .settings(
    name := "play-backend",
    libraryDependencies ++= Dependencies.frontend,
    routesGenerator := InjectedRoutesGenerator,
    commonSettings
  )
