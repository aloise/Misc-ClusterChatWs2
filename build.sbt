name := "chat-cluster-ws"

version := "1.0.0"

scalaVersion := "2.11.7"

// lazy val root = (project in file(".")).enablePlugins(PlayScala).settings( autoScalaLibrary := false )

resolvers ++= Seq(
  "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
  "Kaliber Internal Repository" at "https://jars.kaliber.io/artifactory/libs-release-local",
  "Twitter Maven Repo" at "http://maven.twttr.com/"
)


libraryDependencies ++= Seq(
  cache,
  ws,
  filters,
  "commons-io" % "commons-io" % "2.4",
  "org.reactivemongo" %% "play2-reactivemongo" % "0.11.10",
  "com.github.fdimuccio" %% "play2-sockjs" % "0.4.0",
  "com.softwaremill.macwire" %% "macros" % "0.7.2",
  "org.julienrf" %% "play-json-variants" % "2.0",
  "net.sf.uadetector" % "uadetector-resources" % "2014.10",
  "com.netaporter" %% "scala-uri" % "0.4.3",
  "joda-time" % "joda-time" % "2.9.2",
  "commons-io" % "commons-io" % "2.4",
  "com.sksamuel.scrimage" %% "scrimage-core" % "2.1.0",
  "com.github.tototoshi" %% "scala-csv" % "1.2.2",
  "com.typesafe.play" %% "play-mailer" % "4.0.0-M1",
  "org.cvogt" %% "play-json-extensions" % "0.6.1",
  "org.apache.commons" % "commons-lang3" % "3.4"

)

