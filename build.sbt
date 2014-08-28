name := "tcpproxy"

version := "0.0.1"

scalaVersion := "2.10.4"

scalacOptions ++= Seq("-deprecation", "-feature")

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.3.5",
  "com.typesafe.akka" %% "akka-slf4j" % "2.3.5",
  "ch.qos.logback" %  "logback-classic" % "1.0.13"
)

fork in run := true
