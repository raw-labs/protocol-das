import sbt.Keys._
import sbt._

import java.nio.file.Paths

ThisBuild / credentials += Credentials(
  "GitHub Package Registry",
  "maven.pkg.github.com",
  "raw-labs",
  sys.env.getOrElse("GITHUB_TOKEN", "")
)

ThisBuild / homepage := Some(url("https://www.raw-labs.com/"))
ThisBuild / organization := "com.raw-labs"
ThisBuild / organizationName := "RAW Labs SA"
ThisBuild / organizationHomepage := Some(url("https://www.raw-labs.com/"))
ThisBuild / updateOptions := updateOptions.in(Global).value.withCachedResolution(true)
ThisBuild / resolvers ++= Seq(Resolver.mavenLocal)
ThisBuild / resolvers += "GHR snapi repo" at "https://maven.pkg.github.com/raw-labs/snapi"

ThisBuild / scalaVersion := "2.12.18"
ThisBuild / javacOptions ++= Seq(
              "-source",
              "21",
              "-target",
              "21"
            )

ThisBuild / versionScheme := Some("early-semver")
ThisBuild / publish / skip := false
ThisBuild / publishMavenStyle := true
ThisBuild / publishTo := Some("GitHub raw-labs Apache Maven Packages" at "https://maven.pkg.github.com/raw-labs/protocol-das")

lazy val root = (project in file("."))
  .enablePlugins(ProtobufPlugin)
  .settings(
    name := "protocol-das",
    protobufGrpcEnabled := true,
    // Set fixed versions
    ProtobufConfig / version := "3.25.4",
    ProtobufConfig / protobufGrpcVersion := "1.62.2",
    libraryDependencies ++= Seq(
      "com.raw-labs" %% "protocol-raw" % "0.39.0" % "compile->compile;test->test",
      // Import protobuf files from upstream package, so we can refer to them from our own
      "com.raw-labs" %% "protocol-raw" % "0.39.0" % ProtobufConfig.name,
      // Required for gRPC Protobuf
      "javax.annotation" % "javax.annotation-api" % "1.3.2",
      "io.grpc" % "grpc-netty" % (ProtobufConfig / protobufGrpcVersion).value,
      "io.grpc" % "grpc-protobuf" % (ProtobufConfig / protobufGrpcVersion).value,
      "io.grpc" % "grpc-stub" % (ProtobufConfig / protobufGrpcVersion).value
    )
  )
