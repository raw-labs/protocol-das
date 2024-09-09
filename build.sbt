import sbt.Keys._
import sbt._

import java.nio.file.Paths

ThisBuild / credentials += Credentials(
  "GitHub Package Registry",
  "maven.pkg.github.com",
  "raw-labs",
  sys.env.getOrElse("GITHUB_TOKEN", "")
)

val isRelease = sys.props.getOrElse("release", "false").toBoolean

lazy val commonSettings = Seq(
  homepage := Some(url("https://www.raw-labs.com/")),
  organization := "com.raw-labs",
  organizationName := "RAW Labs SA",
  organizationHomepage := Some(url("https://www.raw-labs.com/")),
  // Use cached resolution of dependencies
  // http://www.scala-sbt.org/0.13/docs/Cached-Resolution.html
  updateOptions := updateOptions.in(Global).value.withCachedResolution(true),
  resolvers ++= Seq(Resolver.mavenLocal),
  resolvers += "GHR snapi repo" at "https://maven.pkg.github.com/raw-labs/snapi",
  resolvers ++= Resolver.sonatypeOssRepos("snapshots"),
  resolvers ++= Resolver.sonatypeOssRepos("releases")
)

lazy val buildSettings = Seq(
  scalaVersion := "2.12.18",
  isSnapshot := !isRelease,
  javacOptions ++= Seq(
    "-source",
    "21",
    "-target",
    "21"
  ),
  scalacOptions ++= Seq(
    "-feature",
    "-unchecked",
    // When compiling in encrypted drives in Linux, the max size of a name is reduced to around 140
    // https://unix.stackexchange.com/a/32834
    "-Xmax-classfile-name",
    "140",
    "-deprecation",
    "-Xlint:-stars-align,_",
    "-Ywarn-dead-code",
    "-Ywarn-macros:after", // Fix for false warning of unused implicit arguments in traits/interfaces.
    "-Ypatmat-exhaust-depth",
    "160"
  )
)

lazy val compileSettings = Seq(
  Compile / doc / sources := Seq.empty,
  Compile / packageDoc / mappings := Seq(),
  Compile / packageSrc / publishArtifact := true,
  Compile / packageDoc / publishArtifact := false,
  Compile / packageBin / packageOptions += Package.ManifestAttributes(
    "Automatic-Module-Name" -> name.value.replace('-', '.')
  ),
  // Ensure Java annotations get compiled first, so that they are accessible from Scala.
  compileOrder := CompileOrder.JavaThenScala
)

lazy val testSettings = Seq(
  // Ensuring tests are run in a forked JVM for isolation.
  Test / fork := true,
  // Disabling parallel execution of tests.
  //Test / parallelExecution := false,
  // Pass system properties starting with "raw." to the forked JVMs.
  Test / javaOptions ++= {
    import scala.collection.JavaConverters._
    val props = System.getProperties
    props
      .stringPropertyNames()
      .asScala
      .filter(_.startsWith("raw."))
      .map(key => s"-D$key=${props.getProperty(key)}")
      .toSeq
  },
  // Set up heap dump options for out-of-memory errors.
  Test / javaOptions ++= Seq(
    "-XX:+HeapDumpOnOutOfMemoryError",
    s"-XX:HeapDumpPath=${Paths.get(sys.env.getOrElse("SBT_FORK_OUTPUT_DIR", "target/test-results")).resolve("heap-dumps")}"
  ),
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
