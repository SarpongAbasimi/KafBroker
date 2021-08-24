import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits._

import scala.concurrent.duration._
import vulcan.Codec
import vulcan.{AvroError}
import fs2.kafka._
import vulcan.{AvroSettings, SchemaRegistryClientSettings, avroDeserializer, avroSerializer}
import fs2.Stream

/** Person Event**/
case class Name(name: String) extends AnyVal
case class Age(age: Int) extends AnyVal
case class PersonEvent(name: String, age: Int, country: String)

sealed trait OperationType
final case object NewSubscription extends OperationType
final case object DeleteSubscription extends OperationType

final case class Organization(organization: String) extends AnyVal
final case class Repository(repository: String) extends AnyVal

final case class Message(
                          operationType: OperationType,
                          organization: Organization,
                          repository: Repository
                        )

/**Need to serialise this personEvent**/


object Fs2Kafka extends IOApp {

  /** Serialising with Avro **/
  implicit val personCodec : Codec[PersonEvent] = Codec.record[PersonEvent](
    name = "PersonEvent",
    namespace = "com.githubDummyProject",
  ){ fields => (
    fields("name", _.name),
    fields("age", _.age),
    fields("country", _.country)
    ).mapN(PersonEvent(_,_, _))
  }

  implicit val organizationCodec: Codec[Organization] = Codec[String].imap(Organization(_))(_.organization)
  implicit val repositoryCodec: Codec[Repository] = Codec[String].imap(Repository(_))(_.repository)

  implicit val operationTypeCodec: Codec[OperationType] = Codec.enumeration[OperationType](
    name = "OperationType",
    namespace = "OperationTypeCodec",
    doc = Some("Operation type"),
    symbols = List("NewSubscription", "DeleteSubscription"),
    encode = {
      case NewSubscription => "NewSubscription"
      case DeleteSubscription => "DeleteSubscription"
    },
    decode = {
      case "NewSubscription" => Right(NewSubscription)
      case "DeleteSubscription" => Right(DeleteSubscription)
      case other => Left(AvroError(s"Something is wrong ${other}"))
    }
  )

  implicit val messageCodec: Codec[Message] = Codec.record[Message](
    name = "Message",
    namespace = "com.Message"
  ){
    fields => (
      fields("operationType", _.operationType),
      fields("organization", _.organization.organization),
      fields("repository", _.repository.repository)
    ).mapN((
             operationType, o, r) => Message(operationType, Organization(o), Repository(r)))
  }

  val exOne: Message = Message(NewSubscription, Organization("47Deg"), Repository("Dummy"))

  val avroSettings = AvroSettings {
    SchemaRegistryClientSettings[IO]("http://localhost:8081")
  }

  implicit val personEventSerializer: RecordSerializer[IO, PersonEvent] =
    avroSerializer[PersonEvent].using(avroSettings)

  implicit val personEventDeSerializer: RecordDeserializer[IO, PersonEvent] =
    avroDeserializer[PersonEvent].using(avroSettings)

  implicit val messageEventSerializer: RecordSerializer[IO, Message] =
    avroSerializer[Message].using(avroSettings)

  override def run(args: List[String]): IO[ExitCode] = {
    def processRecord(record: ConsumerRecord[Unit, String]): IO[(Unit, String)] =
      IO.pure(record.key -> record.value)

    val consumerSettings =
      ConsumerSettings[IO, Unit, String]
        .withAutoOffsetReset(AutoOffsetReset.Earliest)
        .withBootstrapServers("localhost:9092")
        .withGroupId("group")

    val producerSettings =
      ProducerSettings[IO, Unit, Message](
        keySerializer = Serializer[IO, Unit],
        valueSerializer = messageEventSerializer
      )
        .withBootstrapServers("localhost:9092")

    val stream: Stream[IO, Unit] =
      KafkaConsumer.stream(consumerSettings)
        .evalTap(_.subscribeTo("topic"))
        .flatMap(_.stream)
        .mapAsync(25) { committable =>
          processRecord(committable.record)
            .map { case (key, value) =>
              val record = ProducerRecord("topic", key,  exOne)
              ProducerRecords.one(record, committable.offset)
            }
        }
        .through(KafkaProducer.pipe(producerSettings))
        .map(_.passthrough)
        .through(commitBatchWithin(500, 15.seconds))

    val kafkaProducer: Stream[IO, IO[ProducerResult[Unit, Message, Unit]]] = for {
     a <- KafkaProducer.stream[IO, Unit, Message](producerSettings)
     record <- Stream.eval(IO(ProducerRecord("messageTopic", (), exOne)))
     producerRecords <- Stream.eval(IO(println("Publishing message")) >> IO(ProducerRecords.one(record)))
     s <- Stream.eval(a.produce(producerRecords))
    } yield s

    kafkaProducer.compile.drain.as(ExitCode.Success)
//    stream.compile.drain.as(ExitCode.Success)
  }
}
