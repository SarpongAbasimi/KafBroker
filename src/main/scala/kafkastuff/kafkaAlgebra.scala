package kafkastuff

import cats.implicits.catsSyntaxTuple3Semigroupal
import fs2.kafka.{KafkaProducer, ProducerRecord, ProducerRecords, ProducerSettings, RecordSerializer, Serializer}
import fs2.kafka.vulcan.{Auth, AvroSettings, SchemaRegistryClientSettings, avroSerializer}
import fs2.Stream
import kafkastuff.Utils.{BootstrapServer, DeleteSubscription, MessageEvent, NewSubscription, OperationType, Organization, Password, Repository, SchemaRegistryUrl, Topic, UserName}
import vulcan.{AvroError, Codec}
import cats.implicits._
import cats.effect.{
  ConcurrentEffect,
  ContextShift,
  ExitCode,
  IO,
  IOApp,
  Sync
}

case class KafkaConfig(
                        topic: Topic,
                        bootstrapServer: BootstrapServer,
                        schemaRegistryUrl: SchemaRegistryUrl,
                        userName: UserName,
                        password: Password
                      )

trait KafkaAlgebra[F[_], K, V] {

  def publish(key: K, messageEvent: V): Stream[F, Unit]
  def producerSettings: ProducerSettings[F, K, V]
  def avroSettings(
                    schemaRegistryUrl: SchemaRegistryUrl,
                    userName: UserName,
                    password: Password
                  )(implicit
                    sync: Sync[F]
                  ): AvroSettings[F] =
    AvroSettings {
      SchemaRegistryClientSettings[F](schemaRegistryUrl.schemaRegistryUrl)
        .withAuth(Auth.Basic(userName.userName, password.password))
    }
}

object KafkaImplementation extends IOApp {
  implicit val operationTypeCodec: Codec[OperationType] = Codec.enumeration[OperationType](
    name = "operationType",
    namespace = "com.OperationType",
    symbols = List("NewSubscription", "DeleteSubscription"),
    encode = {
      case NewSubscription    => "NewSubscription"
      case DeleteSubscription => "DeleteSubscription"
    },
    decode = {
      case "NewSubscription"    => Right(NewSubscription)
      case "DeleteSubscription" => Right(DeleteSubscription)
      case other                => Left(AvroError(s"$other is not an operationType"))
    }
  )

  implicit val messageEventCodec: Codec[MessageEvent] = Codec.record[MessageEvent](
    name = "MessageEvent",
    namespace = "com.MessageEvent"
  ) { fields =>
    (
      fields("operationType", _.operationType),
      fields("organization", _.organization.organization),
      fields("respository", _.repository.repository)
      ).mapN((organizationType, organisation, repository) =>
      MessageEvent(
        organizationType,
        Organization(organisation),
        Repository(repository)
      )
    )
  }

  def imp[F[_]: Sync: ConcurrentEffect: ContextShift](
                                                       kafkaConfig: KafkaConfig
                                                     ): KafkaAlgebra[F, Int, MessageEvent] =
    new KafkaAlgebra[F, Int, MessageEvent] {
      implicit val messageEventSerializer: RecordSerializer[F, MessageEvent] =
        avroSerializer[MessageEvent].using(
          avroSettings(
            SchemaRegistryUrl(kafkaConfig.schemaRegistryUrl.schemaRegistryUrl),
            UserName(kafkaConfig.userName.userName),
            Password(kafkaConfig.password.password)
          )
        )

      def producerSettings: ProducerSettings[F, Int, MessageEvent] =
        ProducerSettings[F, Int, MessageEvent](
          keySerializer = Serializer[F, Int],
          valueSerializer = messageEventSerializer
        )
          .withBootstrapServers(
            kafkaConfig.bootstrapServer.bootstrapServer
          )

      def publish(
                   key: Int,
                   messageEvent: MessageEvent
                 ): Stream[F, Unit] = for {
        kafkaProducer: KafkaProducer.Metrics[F, Int, MessageEvent] <- KafkaProducer.stream(
          producerSettings
        )
        record: ProducerRecord[Int, MessageEvent] <- Stream.eval(
          Sync[F].delay(
            ProducerRecord[Int, MessageEvent](
              kafkaConfig.topic.topic,
              key,
              messageEvent
            )
          )
        )
        _ <- Stream.eval(
          kafkaProducer.produce(ProducerRecords.one(record)) *> Sync[F].delay(
            println(s"Message Event has been emitted to ${kafkaConfig.topic.topic}")
          )
        )
      } yield ()
    }

  def run(args: List[String]): IO[ExitCode] =  {
     val config = KafkaConfig(
      Topic("dummyProject"),
      BootstrapServer("http://localhost:9092"),
      SchemaRegistryUrl("http://localhost:8081"),
      UserName("Ben"),
      Password("password")
    )
    val implementation = imp[IO](config)
    val operationType = NewSubscription
    val organization  = Organization("47Degrees")
    val repository    = Repository("Scala Exercise")
    val messageEvent  = MessageEvent(operationType, organization, repository)

    implementation.publish(1, messageEvent).compile.drain.as(ExitCode.Success)
  }
}
