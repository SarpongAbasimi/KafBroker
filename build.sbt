name := "kafka"

version := "0.1"

scalaVersion := "2.13.6"
lazy val typeSafeConfVersion = "1.4.1"
lazy val pureConfigVersion = "0.13.0"
lazy val kafkaClientVersion = "2.7.0"
lazy val fs2Kafka = "1.7.0"
lazy val fs2KafkaVulcan = "1.7.0"
lazy val vulcanVersion = "1.7.1"

libraryDependencies ++= Seq(
  "com.typesafe" % "config" % typeSafeConfVersion,
  "com.github.pureconfig" %% "pureconfig" % pureConfigVersion,
  "org.apache.kafka" % "kafka-clients" % kafkaClientVersion,
  "com.fasterxml.jackson.core" % "jackson-databind" % "2.12.3",
  "com.github.fd4s" %% "fs2-kafka" % fs2Kafka,
  "com.github.fd4s" %% "fs2-kafka-vulcan" % fs2KafkaVulcan,
  "com.github.fd4s" %% "vulcan" % vulcanVersion
)
resolvers += "confluent" at "https://packages.confluent.io/maven/"