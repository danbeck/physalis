import sbt.Keys._

name := """physalis"""

version := "0.0.1"

lazy val root = (project in file(".")).enablePlugins(play.PlayScala)

scalaVersion := "2.10.4"

libraryDependencies ++= Seq(
  "securesocial" %% "securesocial" % "2.1.2"
)

resolvers ++= Seq(
    "Apache" at "http://repo1.maven.org/maven2/",
    "jBCrypt Repository" at "http://repo1.maven.org/maven2/org/",
    "Sonatype OSS Snasphots" at "http://oss.sonatype.org/content/repositories/snapshots"
)

resolvers += Resolver.url("SecureSocial Repository",
	url("http://repo.scala-sbt.org/scalasbt/sbt-plugin-releases/")
)(Resolver.ivyStylePatterns)

