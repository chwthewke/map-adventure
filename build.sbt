import sbt._
import sbt.Keys._
import sbtcrossproject.CrossProject

ThisBuild / organization := "net.chwthewke"

ThisBuild / ideBasePackages.withRank( KeyRanks.Invisible ) := Seq( "net.chwthewke.mapadv" )

ThisBuild / Compile / doc / sources                := Seq.empty
ThisBuild / Compile / packageDoc / publishArtifact := false

enablePlugins( Scalafmt )
enablePlugins( Dependencies )

val sharedSettings = Seq(
  scalaVersion                                          := "3.7.1",
  ideExcludedDirectories.withRank( KeyRanks.Invisible ) := Seq( target.value )
)

val aggregateSettings = Seq(
  publish      := {},
  publishLocal := {}
)

val `map-adventure-core-cross`: CrossProject =
  crossProject( JSPlatform, JVMPlatform )
    .crossType( CrossType.Pure )
    .in( file( "core" ) )
    .settings( sharedSettings )
    .settings( cats, kittens, mouse, catsTime, circe )
    .enablePlugins( Scalac )

val `map-adventure-core-js`: Project  = `map-adventure-core-cross`.js
val `map-adventure-core-jvm`: Project = `map-adventure-core-cross`.jvm

val `map-adventure-core`: Project =
  project
    .in( file( "core/target" ) )
    .settings( sharedSettings )
    .settings( aggregateSettings )
    .aggregate( `map-adventure-core-js`, `map-adventure-core-jvm` )

val `map-adventure-tools`: Project =
  project
    .in( file( "tools" ) )
    .settings( sharedSettings )
    .enablePlugins( Scalac )
    .settings( http4sEmberClient, http4sCirce, logging, circeParser )
    .dependsOn( `map-adventure-core-jvm` )

val `map-adventure-assets-cross`: CrossProject =
  crossProject( JSPlatform, JVMPlatform )
    .crossType( CrossType.Pure )
    .in( file( "assets" ) )
    .settings( sharedSettings )
    .enablePlugins( Scalac )

val `map-adventure-assets-jvm`: Project = `map-adventure-assets-cross`.jvm
val `map-adventure-assets-js`: Project  = `map-adventure-assets-cross`.js

val `map-adventure-assets`: Project =
  project
    .in( file( "assets/target" ) )
    .settings( sharedSettings )
    .settings( aggregateSettings )
    .aggregate( `map-adventure-assets-js`, `map-adventure-assets-jvm` )

val `map-adventure-server`: Project = project
  .in( file( "server" ) )
  .enablePlugins( Scalac )
  .enablePlugins( BuildInfo )
  .settings( buildInfoPackage := "net.chwthewke.mapadv.server" )
  .settings( sharedSettings )
  .dependsOn( `map-adventure-core-jvm` )
  .settings(
    circeParser,
    http4sCore,
    http4sDsl,
    http4sEmberServer,
    http4sCirce,
    scalatags,
    http4sScalatags,
    pureconfig,
    pureconfigCatsEffect,
    pureconfigFs2,
    pureconfigIp4s,
    pureconfigHttp4s,
    logging
  )

val backendRunnerSettings: Seq[Def.Setting[_]] = Seq(
  Compile / mainClass  := Some( "net.chwthewke.mapadv.server.Main" ),
  Compile / run / fork := true
)

// NOTE this module is intended for running the backend from sbt or IntelliJ
//  it could have specific application.conf/logback.xml/assets etc.,
//  matching the requirements for map-adventure-frontend-run
val `map-adventure-server-run`: Project =
  project
    .in( file( "server-run" ) )
    .enablePlugins( Scalac )
    .settings( sharedSettings )
    .settings( backendRunnerSettings )
    .dependsOn( `map-adventure-server` )

val `map-adventure-frontend`: Project =
  project
    .in( file( "frontend" ) )
    .enablePlugins( Scalac )
    .enablePlugins( ScalaJSPlugin )
    .enablePlugins( BuildInfo )
    .settings( buildInfoPackage := "net.chwthewke.mapadv.spa" )
    .settings( sharedSettings )
    .settings( scalaJSLinkerConfig ~= { _.withModuleKind( ModuleKind.ESModule ) } )
    .settings( tyrian, http4sCore, http4sDom, http4sCirce )
    .dependsOn( `map-adventure-core-js`, `map-adventure-assets-js` )

// NOTE this module is intended for running the frontend from sbt or a terminal
//  with a hot-reload capable dev webserver (via npm scripts using parcel)
val `map-adventure-frontend-run`: Project = project
  .in( file( "frontend-run" ) )
  .enablePlugins( Scalac )
  .enablePlugins( ScalaJSPlugin )
  .settings( sharedSettings )
  .settings(
    ideExcludedDirectories ++=
      Seq( ".parcel-cache", "dist", "node_modules" ).map( n => baseDirectory.value / n )
  )
  .settings( scalaJSLinkerConfig ~= { _.withModuleKind( ModuleKind.ESModule ) } )
  .settings( circeParser )
  .enablePlugins( FrontendDev )
  .dependsOn( `map-adventure-frontend` )

// NOTE this module is the "production" app, which contains the server and serves the frontend assets
val `map-adventure-app`: Project =
  project
    .in( file( "app" ) )
    .enablePlugins( Scalac )
    .enablePlugins( JavaServerAppPackaging )
    .enablePlugins( LauncherJarPlugin )
    .settings( sharedSettings )
    .settings( backendRunnerSettings )
    .settings( Packaging.settings( frontendProject = `map-adventure-frontend` ) )
    .dependsOn( `map-adventure-server` )

val `map-adventure-tests` = project
  .in( file( "tests" ) )
  .settings( sharedSettings )
  .settings( scalatest, scalacheck )
  .dependsOn( `map-adventure-core-jvm`, `map-adventure-server` )
  .enablePlugins( Scalac )

val `map-adventure` = project
  .in( file( "." ) )
  .settings( sharedSettings, aggregateSettings )
  .aggregate(
    `map-adventure-core`,
    `map-adventure-tools`,
    `map-adventure-assets`,
    `map-adventure-server`,
    `map-adventure-server-run`,
    `map-adventure-frontend`,
    `map-adventure-frontend-run`,
    `map-adventure-app`,
    `map-adventure-tests`
  )
