name := "redisAkkaPersistenceExample"

version := "1.0"

scalaVersion := "2.11.8"

resolvers += Resolver.jcenterRepo

val akkaVersion = "2.4.2"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
  "com.typesafe.akka" %% "akka-persistence" % akkaVersion,
  "com.typesafe.akka" %% "akka-camel" % akkaVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test",

  "ch.qos.logback" % "logback-classic" % "1.1.2",

  "org.scalatest" %% "scalatest" % "2.2.4" % "test"
)
