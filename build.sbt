name := """private"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.1"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache,
  ws,
  "com.foundationdb" % "fdb-sql-parser" % "1.5.0",
  "net.debasishg" %% "redisclient" % "2.13",
  "com.github.tototoshi" %% "scala-csv" % "1.0.0",
  "org.postgresql" % "postgresql" % "9.3-1102-jdbc41"
)
