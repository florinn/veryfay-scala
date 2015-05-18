name := "veryfay"

organization := "com.github.florinn"

version := "0.1-SNAPSHOT"

scalaVersion := "2.11.6"

libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value

libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.4" % "test"

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}

publishMavenStyle := true

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

description := "Activity based authorization library for Scala"

homepage := Some(url("https://github.com/florinn/veryfay-scala"))

startYear := Some(2015)

licenses := Seq("MIT license" -> url("https://github.com/florinn/veryfay-scala/blob/master/LICENSE"))

pomExtra := (
  <developers>
    <developer>
	  <name>Florin Nitoi</name>
	  <email>florin.nitoi@gmail.com</email>
    </developer>
  </developers>
  <scm>
    <url>git@github.com:florinn/veryfay-scala.git</url>
	<connection>scm:git:git@github.com:florinn/veryfay-scala.git</connection>
  </scm> )