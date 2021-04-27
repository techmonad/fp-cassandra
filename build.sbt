import sbtrelease.ReleaseStateTransformations._
import sbtrelease.ReleasePlugin.autoImport._

val circeVersion = "0.13.0"

// -------------------------------------------------------------------------------------------------------------------
// Root Project
// -------------------------------------------------------------------------------------------------------------------
lazy val root = (project in file("."))
  .settings(
    inThisBuild(
      List(
        organization := "com.techmonal",
        scalaVersion := "2.13.5",
        scalastyleFailOnError := true,
        scalastyleFailOnWarning := false,
        scalafmtOnCompile := true
      )
    ),
    name := "fp-cassandra"
  )
  .aggregate(common, db, web)
  .dependsOn(common, db, web)

// -------------------------------------------------------------------------------------------------------------------
// Common Module
// -------------------------------------------------------------------------------------------------------------------
lazy val common = project
  .in(file("modules/common"))
  .settings(name := "common")
  .settings(addCompilerPlugin(kindProjectorSetting))
  .settings(libraryDependencies ++= commonLibraryDependencies)

// -------------------------------------------------------------------------------------------------------------------
// Web Module
// -------------------------------------------------------------------------------------------------------------------
lazy val web = project
  .in(file("modules/web"))
  .settings(name := "web")
  .aggregate(common)
  .dependsOn(common)
  .settings(addCompilerPlugin(kindProjectorSetting))
  .settings(libraryDependencies ++= webLibraryDependencies)

// -------------------------------------------------------------------------------------------------------------------
// DB Module
// -------------------------------------------------------------------------------------------------------------------
lazy val db = project
  .in(file("modules/db"))
  .settings(name := "db")
  .aggregate(common)
  .dependsOn(common)
  .settings(addCompilerPlugin(kindProjectorSetting))
  .settings(libraryDependencies ++= dbLibraryDependencies)

lazy val commonLibraryDependencies = Seq(
  "com.twitter"       %% "util-core"                % "21.3.0",
  "ch.qos.logback"    % "logback-classic"           % "1.2.3",
  "org.scalatest"     %% "scalatest"                % "3.2.8" % Test,
  "org.scalatestplus" %% "scalatestplus-scalacheck" % "3.1.0.0-RC2" % Test
) ++ circeDependencies

lazy val webLibraryDependencies = Seq()

lazy val dbLibraryDependencies = commonLibraryDependencies ++ Seq(
  "com.datastax.dse"  % "dse-java-driver-core" % "1.9.0",
  "org.typelevel"     %% "cats-core"           % "2.6.0",
  "org.cassandraunit" % "cassandra-unit"       % "4.3.1.0"
)

lazy val circeDependencies = Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser"
).map(_ % circeVersion)

scalacOptions ++= Seq(
  "-feature",
  "-unchecked",
  "-language:higherKinds",
  "-language:postfixOps",
  "-deprecation",
  "-Ypartial-unification",
  "-encoding"
)

resolvers ++= Seq(
  Resolver.mavenLocal,
  Resolver.sonatypeRepo("releases"),
  Resolver.sonatypeRepo("snapshots"),
  "Bintray ".at("https://dl.bintray.com/projectseptemberinc/maven")
)

releaseProcess := Seq(
  checkSnapshotDependencies,
  inquireVersions,
  // publishing locally in the process
  releaseStepCommandAndRemaining("+publishLocal"),
  releaseStepCommandAndRemaining("+clean"),
  releaseStepCommandAndRemaining("+test"),
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  setNextVersion,
  commitNextVersion,
  pushChanges
)

lazy val kindProjectorSetting = "org.typelevel" %% "kind-projector" % "0.11.3" cross CrossVersion.full

addCommandAlias("fmt", ";scalafmtSbt;scalafmt;test:scalafmt")
addCommandAlias("cpl", ";compile;test:compile")
addCommandAlias("validate", ";clean;scalafmtSbtCheck;scalafmtCheck;test:scalafmtCheck;coverage;test;coverageOff;coverageReport;coverageAggregate")
addCommandAlias("testAll", ";clean;test;it:test")
