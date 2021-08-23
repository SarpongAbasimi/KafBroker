import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits._
import scala.concurrent.duration._
import vulcan.Codec
import fs2.kafka._

/** Person Event**/
case class Name(name: String) extends AnyVal
case class Age(age: Int) extends AnyVal
case class PersonEvent(name: Name, age: Age)

/**Need to serialise this personEvent**/

/** Serialising with Avro **/
object Main {
  val personCodec : Codec[PersonEvent] =Codec.record[PersonEvent](
    name = "PersonEvent",
    namespace = "com.githubDummyProject",
  ){ fields => (
    fields("name", _.name.name),
    fields("age", _.age.age)
    ).mapN((a,b)=> PersonEvent(Name(a), Age(b)))
  }
  def main(args: Array[String]): Unit = {
    println(personCodec.schema)
  }
}

object Fs2Kafka extends IOApp {
  override def run(args: List[String]): IO[ExitCode] = {
    def processRecord(record: ConsumerRecord[Unit, String]): IO[(Unit, String)] =
      IO.pure(record.key -> record.value)

    val consumerSettings =
      ConsumerSettings[IO, Unit, String]
        .withAutoOffsetReset(AutoOffsetReset.Earliest)
        .withBootstrapServers("localhost:9092")
        .withGroupId("group")

    val producerSettings =
      ProducerSettings[IO, Unit, String]
        .withBootstrapServers("localhost:9092")

    val stream =
      KafkaConsumer.stream(consumerSettings)
        .evalTap(_.subscribeTo("topic"))
        .flatMap(_.stream)
        .mapAsync(25) { committable =>
          processRecord(committable.record)
            .map { case (key, value) =>
              val record = ProducerRecord("topic", key, value)
              ProducerRecords.one(record, committable.offset)
            }
        }
        .through(KafkaProducer.pipe(producerSettings))
        .map(_.passthrough)
        .through(commitBatchWithin(500, 15.seconds))

    stream.compile.drain.as(ExitCode.Success)
  }
}
