import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}

import java.util.Properties

object Main {
  lazy val kafkaProps = new Properties()
  lazy val configurations = KafkaConfig.conf("kafka.conf", "application.kafka")
  kafkaProps.put(
    configurations.bootsStrapServer.bootStrapServer,
    configurations.bootsStrapServerUrl.bootsStrapServerUrl
  )
  kafkaProps.put(
    configurations.keySerializer.keySerializer,
    "org.apache.kafka.common.serialization.StringSerializer"
  )
  kafkaProps.put(
    configurations.valueSerializer.valueSerializer,
    "org.apache.kafka.common.serialization.StringSerializer"
  )

  lazy val producerRecord = new ProducerRecord[String, String](
    configurations.topicName.topicName,
    3,
    "key",
    "First Message"
  )

  def main(args: Array[String]): Unit = {
    lazy val kafkaProducer = new KafkaProducer[String, String](kafkaProps)
    kafkaProducer.send(producerRecord).get()
  }
}
