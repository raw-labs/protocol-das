import sbt.Keys._
import sbt._

import java.nio.file.Paths

ThisBuild / credentials += Credentials(
  "GitHub Package Registry",
  "maven.pkg.github.com",
  "raw-labs",
  sys.env.getOrElse("GITHUB_TOKEN", "")
)

lazy val commonSettings = Seq(
  homepage := Some(url("https://www.raw-labs.com/")),
  organization := "com.raw-labs",
  organizationName := "RAW Labs SA",
  organizationHomepage := Some(url("https://www.raw-labs.com/")),
  // Use cached resolution of dependencies
  // http://www.scala-sbt.org/0.13/docs/Cached-Resolution.html
  updateOptions := updateOptions.in(Global).value.withCachedResolution(true),
  resolvers += "RAW Labs GitHub Packages" at "https://maven.pkg.github.com/raw-labs/_"
)

lazy val buildSettings = Seq(
  scalaVersion := "2.13.15"
)

lazy val compileSettings = Seq(
  Compile / doc / sources := Seq.empty,
  Compile / packageDoc / mappings := Seq(),
  Compile / packageSrc / publishArtifact := true,
  Compile / packageDoc / publishArtifact := false,
  Compile / packageBin / packageOptions += Package.ManifestAttributes(
    "Automatic-Module-Name" -> name.value.replace('-', '.')
  )
)

lazy val testSettings = Seq(
  // Ensuring tests are run in a forked JVM for isolation.
  Test / fork := true,
  // Required for publishing test artifacts.
  Test / publishArtifact := true
)

val isCI = sys.env.getOrElse("CI", "false").toBoolean

lazy val publishSettings = Seq(
  versionScheme := Some("early-semver"),
  publish / skip := false,
  publishMavenStyle := true,
  publishTo := Some("GitHub raw-labs Apache Maven Packages" at "https://maven.pkg.github.com/raw-labs/protocol-das"),
  publishConfiguration := publishConfiguration.value.withOverwrite(isCI)
)

lazy val strictBuildSettings = commonSettings ++ compileSettings ++ buildSettings ++ testSettings ++ Seq(
  scalacOptions ++= Seq(
    "-Xfatal-warnings"
  )
)

lazy val root = (project in file("."))
  .enablePlugins(ProtobufPlugin)
  .settings(
    name := "protocol-das",
    strictBuildSettings,
    publishSettings,
    protobufGrpcEnabled := true,
    // Set fixed versions
    ProtobufConfig / version := "3.25.4",
    ProtobufConfig / protobufGrpcVersion := "1.62.2",
    libraryDependencies ++= Seq(
      // Required for gRPC Protobuf
      "javax.annotation" % "javax.annotation-api" % "1.3.2",
      "io.grpc" % "grpc-netty" % (ProtobufConfig / protobufGrpcVersion).value,
      "io.grpc" % "grpc-protobuf" % (ProtobufConfig / protobufGrpcVersion).value,
      "io.grpc" % "grpc-stub" % (ProtobufConfig / protobufGrpcVersion).value
    )
  )
