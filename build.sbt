name := "test"

version := "0.1"

scalaVersion := "2.12.13"

scalacOptions += "-Ypartial-unification"

val AkkaVersion = "2.6.18"
libraryDependencies ++= Seq(
  "org.typelevel" %% "cats-core" % "2.3.0",
  "org.apache.kafka" % "kafka-clients" % "3.1.0",
  "com.typesafe.akka" %% "akka-stream-kafka" % "2.1.0",
  "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
  "com.typesafe.akka" %% "akka-http" % "10.1.12",
  "com.typesafe.slick" %% "slick" % "3.3.3",
  "com.typesafe.slick" %% "slick-hikaricp" % "3.3.3",
  "org.slf4j" % "slf4j-nop" % "1.6.4",
  "com.enragedginger" %% "akka-quartz-scheduler" % "1.9.2-akka-2.6.x"
)

