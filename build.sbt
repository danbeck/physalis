import sbt.Keys._

name := """physalis"""

version := "0.0.1"

lazy val root = (project in file(".")).enablePlugins(play.PlayScala)

scalaVersion := "2.11.4"

libraryDependencies ++= Seq(
   "ws.securesocial" %% "securesocial" % "3.0-M1"
)

resolvers ++= Seq(
    "Apache" at "http://repo1.maven.org/maven2/",
    "jBCrypt Repository" at "http://repo1.maven.org/maven2/org/",
    "Sonatype OSS Snasphots" at "http://oss.sonatype.org/content/repositories/snapshots"
)
