name := "kafka"

version := "0.1"

scalaVersion := "2.13.6"
lazy val avro4sVersion = "4.0.4"
lazy val typeSafeConfVersion = "1.4.1"
lazy val pureConfigVersion = "0.13.0"
lazy val kafkaClientVersion = "2.7.0"

libraryDependencies ++= Seq(
  "com.sksamuel.avro4s" %% "avro4s-core" % avro4sVersion,
  "com.typesafe" % "config" % typeSafeConfVersion,
  "com.github.pureconfig" %% "pureconfig" % pureConfigVersion,
  "org.apache.kafka" % "kafka-clients" % kafkaClientVersion,
  "com.fasterxml.jackson.core" % "jackson-databind" % "2.12.3"
)