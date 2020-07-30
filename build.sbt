lazy val baseSettings = Seq(
    name := "pure-tftp"
  , organization := "ru.pashnik"
  , version := "0.1.0-SNAPSHOT"
  , scalaVersion := "2.13.2"
  , sbtVersion := "1.3.12"
  , scalacOptions ++= Seq(
      "-feature"
    , "-unchecked"
    , "-deprecation"
    , "-Xfatal-warnings"
    , "-Ymacro-annotations"
    , "-language:higherKinds"
    , "-language:implicitConversions"
  )
  , resourceDirectory in Compile := baseDirectory.value / "resources"
)

lazy val scalaTestVersion  = "3.2.0"
lazy val logbackVersion    = "1.2.3"
lazy val catsVersion       = "2.0.0"
lazy val fs2Version        = "2.1.0"
lazy val pureConfigVersion = "0.12.2"
lazy val circeVersion      = "0.12.3"
lazy val coercibleVersion  = "0.4.4"
lazy val scalaCheckVersion = "1.14.1"

lazy val deps = {
  Seq(
      "io.circe" %% "circe-core"
    , "io.circe" %% "circe-generic"
    , "io.circe" %% "circe-parser"
  ).map(_                     % circeVersion) ++ Seq(
      "org.typelevel"         %% "cats-effect" % catsVersion
    , "org.typelevel"         %% "cats-core" % catsVersion
    , "org.scalatest"         %% "scalatest" % scalaTestVersion % "test"
    , "ch.qos.logback"        % "logback-classic" % logbackVersion
    , "com.github.pureconfig" %% "pureconfig" % pureConfigVersion
    , "co.fs2"                %% "fs2-core" % fs2Version
    , "co.fs2"                %% "fs2-io" % fs2Version
    , "io.estatico"           %% "newtype" % coercibleVersion
    , "org.scalacheck"        %% "scalacheck" % scalaCheckVersion % "test"
  )
}.map(_ withSources () withJavadoc ())

lazy val assemblySettings = Seq(
    assemblyJarName in assembly := name.value + ".jar"
  , assemblyMergeStrategy in assembly := {
    case PathList("META-INF", _ @_*) => MergeStrategy.discard
    case _                           => MergeStrategy.first
  }
)

lazy val global = (project in file("."))
  .settings(baseSettings)

lazy val server = project
  .settings(baseSettings, assemblySettings, libraryDependencies ++= deps)
  .dependsOn(utils)

lazy val client = project
  .settings(baseSettings, assemblySettings, libraryDependencies ++= deps)
  .dependsOn(utils)

lazy val utils = project
  .settings(baseSettings, libraryDependencies ++= deps)
