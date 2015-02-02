import sbt.Keys._
import NativePackagerKeys._
import com.typesafe.sbt.SbtNativePackager._


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

maintainer in Docker := "Daniel Beck <d.danielbeck@googlemail.com>"

dockerExposedPorts in Docker := Seq(9000, 9443)

dockerBaseImage := "williamyeh/java8"