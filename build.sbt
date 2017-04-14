name := """oauth-provider-seed"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  jdbc,
  cache,
  ws,
  "org.scalaz"             %% "scalaz-core"                % "7.1.3"           withSources(),
  "joda-time"              %  "joda-time"                  % "2.8.1"           withSources(),
  "com.github.driox"       %% "sorus"                      % "1.0.0"           withSources(),
  "org.postgresql"         %  "postgresql"                 % "9.4-1201-jdbc41" withSources(),
  "org.joda"               %  "joda-convert"               % "1.7"             withSources(),
  "com.github.tototoshi"   %% "slick-joda-mapper"          % "2.1.0"           withSources(),
  "com.typesafe.play"      %% "play-slick"                 % "1.1.1"           withSources(),
  "com.typesafe.play"      %% "play-slick-evolutions"      % "1.1.1"           withSources(),
  "com.typesafe.slick"     %% "slick-codegen"              % "3.1.1"           withSources(),
  "io.kanaka"              %% "play-monadic-actions"       % "1.0.1"           withSources()
)

