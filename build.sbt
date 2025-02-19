ThisBuild / version      := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "3.3.1"
ThisBuild / scalacOptions ++= Seq(
  "-unchecked",
  "-deprecation",
  "-feature",
  "-explain"
)

ThisBuild / testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
ThisBuild / libraryDependencySchemes += "dev.zio" %% "zio-json" % "early-semver"
ThisBuild / dependencyOverrides += "dev.zio" %% "zio-json" % "0.7.21"
ThisBuild / dependencyOverrides +=   "ch.qos.logback" %% "logback-classic" % "1.5.16"

val zioVersion        = "2.1.15"
val tapirVersion      = "1.11.14"
val zioLoggingVersion = "2.4.0"
val zioConfigVersion  = "4.0.3"
val sttpVersion       = "3.10.3"
val javaMailVersion   = "1.6.2"
val stripeVersion     = "24.3.0"

val dependencies = Seq(
  "com.softwaremill.sttp.tapir"   %% "tapir-sttp-client"                 % tapirVersion,
  "com.softwaremill.sttp.tapir"   %% "tapir-json-zio"                    % tapirVersion,
  "com.softwaremill.sttp.client3" %% "zio"                               % sttpVersion,
  "dev.zio"                       %% "zio-json"                          % "0.4.2",
  "com.softwaremill.sttp.tapir"   %% "tapir-zio"                         % tapirVersion,
  "com.softwaremill.sttp.tapir"   %% "tapir-zio-http-server"             % tapirVersion,
  "com.softwaremill.sttp.tapir"   %% "tapir-swagger-ui-bundle"           % tapirVersion,
  "com.softwaremill.sttp.tapir"   %% "tapir-sttp-stub-server"            % tapirVersion % "test",
  "dev.zio"                       %% "zio-streams"                       % zioVersion,
  "dev.zio"                       %% "zio-kafka"                         % "2.10.0",
  "dev.zio"                       %% "zio-logging"                       % "2.1.15",
  "dev.zio"                       %% "zio-logging-slf4j"                 % "2.1.15",
  "dev.zio"                       %% "zio-logging-slf4j2-bridge"         % "2.1.15",
  "dev.zio"                       %% "zio-test"                          % zioVersion,
  "dev.zio"                       %% "zio-test-junit"                    % zioVersion   % "test",
  "dev.zio"                       %% "zio-test-sbt"                      % zioVersion   % "test",
  "dev.zio"                       %% "zio-test-magnolia"                 % zioVersion   % "test",
  "dev.zio"                       %% "zio-mock"                          % "1.0.0-RC9"  % "test",
  "dev.zio"                       %% "zio-config"                        % zioConfigVersion,
  "dev.zio"                       %% "zio-config-magnolia"               % zioConfigVersion,
  "dev.zio"                       %% "zio-config-typesafe"               % zioConfigVersion,
  "io.getquill"                   %% "quill-jdbc-zio"                    % "4.7.3",
  "org.postgresql"                 % "postgresql"                        % "42.5.0",
  "org.flywaydb"                   % "flyway-core"                       % "9.7.0",
  "io.github.scottweaver"         %% "zio-2-0-testcontainers-postgresql" % "0.9.0",
  "dev.zio"                       %% "zio-prelude"                       % "1.0.0-RC16",
  "com.auth0"                      % "java-jwt"                          % "4.2.1",
  "com.sun.mail"                   % "javax.mail"                        % javaMailVersion,
  "com.stripe"                     % "stripe-java"                       % stripeVersion
)


lazy val server = (project in file("modules/server"))
  .settings(
    libraryDependencies ++= dependencies
  )

lazy val `reviews-producer` = (project in file("modules/reviews-producer"))
  .settings(
    libraryDependencies ++= dependencies
  )


lazy val root = (project in file("."))
  .settings(
    name := "scala-zio-tapir"
  )
  .aggregate(server, `reviews-producer`)
  .dependsOn(server, `reviews-producer`)