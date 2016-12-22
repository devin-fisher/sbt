import Dependencies._
import Util._
import com.typesafe.tools.mima.core._, ProblemFilters._

def baseVersion: String = "0.1.0-M16"
def internalPath   = file("internal")

def commonSettings: Seq[Setting[_]] = Seq(
  scalaVersion := scala211,
  // publishArtifact in packageDoc := false,
  resolvers += Resolver.typesafeIvyRepo("releases"),
  resolvers += Resolver.sonatypeRepo("snapshots"),
  resolvers += Resolver.mavenLocal,
  // concurrentRestrictions in Global += Util.testExclusiveRestriction,
  testOptions += Tests.Argument(TestFrameworks.ScalaCheck, "-w", "1"),
  javacOptions in compile ++= Seq("-target", "6", "-source", "6", "-Xlint", "-Xlint:-serial"),
  crossScalaVersions := Seq(scala211, scala212),
  scalacOptions ++= Seq("-Ywarn-unused", "-Ywarn-unused-import"),
  scalacOptions --= // scalac 2.10 rejects some HK types under -Xfuture it seems..
    (CrossVersion partialVersion scalaVersion.value collect { case (2, 10) => List("-Xfuture", "-Ywarn-unused", "-Ywarn-unused-import") }).toList.flatten,
  scalacOptions in console in Compile -= "-Ywarn-unused-import",
  scalacOptions in console in Test    -= "-Ywarn-unused-import",
  mimaPreviousArtifacts := Set(), // Some(organization.value %% moduleName.value % "1.0.0"),
  publishArtifact in Compile := true,
  publishArtifact in Test := false
)

lazy val utilRoot: Project = (project in file(".")).
  aggregate(
    utilInterface, utilControl, utilCollection, utilApplyMacro, utilComplete,
    utilLogging, utilRelation, utilLogic, utilCache, utilTracking, utilTesting,
    utilScripted
  ).
  settings(
    inThisBuild(Seq(
      git.baseVersion := baseVersion,
      bintrayPackage := "util",
      homepage := Some(url("https://github.com/sbt/util")),
      description := "Util module for sbt",
      scmInfo := Some(ScmInfo(url("https://github.com/sbt/util"), "git@github.com:sbt/util.git"))
    )),
    commonSettings,
    name := "Util Root",
    publish := {},
    publishLocal := {},
    publishArtifact in Compile := false,
    publishArtifact in Test := false,
    publishArtifact := false,
    customCommands
  )

// defines Java structures used across Scala versions, such as the API structures and relationships extracted by
//   the analysis compiler phases and passed back to sbt.  The API structures are defined in a simple
//   format from which Java sources are generated by the datatype generator Projproject
lazy val utilInterface = (project in internalPath / "util-interface").
  settings(
    commonSettings,
    javaOnlySettings,
    name := "Util Interface",
    exportJars := true
  )

lazy val utilControl = (project in internalPath / "util-control").
  settings(
    commonSettings,
    name := "Util Control"
  )

lazy val utilCollection = (project in internalPath / "util-collection").
  dependsOn(utilTesting % Test).
  settings(
    commonSettings,
    Util.keywordsSettings,
    name := "Util Collection"
  )

lazy val utilApplyMacro = (project in internalPath / "util-appmacro").
  dependsOn(utilCollection).
  settings(
    commonSettings,
    name := "Util Apply Macro",
    libraryDependencies += scalaCompiler.value
  )

// Command line-related utilities.
lazy val utilComplete = (project in internalPath / "util-complete").
  dependsOn(utilCollection, utilControl, utilTesting % Test).
  settings(
    commonSettings,
    name := "Util Completion",
    libraryDependencies += jline
  ).
  configure(addSbtIO)

// logging
lazy val utilLogging = (project in internalPath / "util-logging").
  dependsOn(utilInterface, utilTesting % Test).
  settings(
    commonSettings,
    crossScalaVersions := Seq(scala210, scala211),
    name := "Util Logging",
    libraryDependencies += jline
  )

// Relation
lazy val utilRelation = (project in internalPath / "util-relation").
  dependsOn(utilTesting % Test).
  settings(
    commonSettings,
    name := "Util Relation"
  )

// A logic with restricted negation as failure for a unique, stable model
lazy val utilLogic = (project in internalPath / "util-logic").
  dependsOn(utilCollection, utilRelation, utilTesting % Test).
  settings(
    commonSettings,
    name := "Util Logic"
  )

// Persisted caching based on sjson-new
lazy val utilCache = (project in internalPath / "util-cache").
  dependsOn(utilCollection, utilTesting % Test).
  settings(
    commonSettings,
    name := "Util Cache",
    libraryDependencies ++= Seq(sjsonnew, scalaReflect.value),
    libraryDependencies += sjsonnewScalaJson % Test
  ).
  configure(addSbtIO)

// Builds on cache to provide caching for filesystem-related operations
lazy val utilTracking = (project in internalPath / "util-tracking").
  dependsOn(utilCache, utilTesting % Test).
  settings(
    commonSettings,
    name := "Util Tracking",
    libraryDependencies += sjsonnewScalaJson % Test
  ).
  configure(addSbtIO)

// Internal utility for testing
lazy val utilTesting = (project in internalPath / "util-testing").
  settings(
    commonSettings,
    crossScalaVersions := Seq(scala210, scala211),
    name := "Util Testing",
    libraryDependencies ++= Seq(scalaCheck, scalatest)
  )

lazy val utilScripted = (project in internalPath / "util-scripted").
  dependsOn(utilLogging).
  settings(
    commonSettings,
    name := "Util Scripted",
    libraryDependencies ++= {
      scalaVersion.value match {
        case sv if sv startsWith "2.11" => Seq(parserCombinator211)
        case sv if sv startsWith "2.12" => Seq(parserCombinator211)
        case _ => Seq()
      }
    }
  ).
  configure(addSbtIO)

def customCommands: Seq[Setting[_]] = Seq(
  commands += Command.command("release") { state =>
    // "clean" ::
    "so compile" ::
    "so publishSigned" ::
    "reload" ::
    state
  }
)
