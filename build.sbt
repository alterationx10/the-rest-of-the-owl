ThisBuild / scalaVersion := "2.13.8"
ThisBuild / organization := "com.alterationx10"

val commonDependencies = Seq(
  "io.d11"        %% "zhttp"          % "2.0.0-RC9", // Brings in ZIO-2.0.0-RC6
  "io.getquill"   %% "quill-zio"      % "4.0.0-RC2", // Uses same ZIO version
  "io.getquill"   %% "quill-jdbc-zio" % "4.0.0-RC2",
  "dev.zio"       %% "zio-json"       % "0.3.0-RC8",
  "com.auth0"      % "java-jwt"       % "4.0.0",
  "com.h2database" % "h2"             % "1.4.199"
)

lazy val basics = project
  .in(file("basics"))
  .settings(
    name := "zhttp-basics",
    libraryDependencies ++= commonDependencies,
    fork := true
  )

lazy val next = project
  .in(file("intermediate"))
  .settings(
    name := "zhttp-next",
    libraryDependencies ++= commonDependencies,
    fork := true
  )

lazy val extra = project
  .in(file("extra"))
  .settings(
    name := "zhttp-extra",
    libraryDependencies ++= commonDependencies,
    fork := true
  )

lazy val troto = project
  .in(file("troto"))
  .settings(
    name := "the-REST-of-the-owl",
    libraryDependencies ++= commonDependencies,
    fork := true
  )
