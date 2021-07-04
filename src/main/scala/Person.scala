import Types.{Age, Name}
import com.sksamuel.avro4s.AvroSchema

case class Person(name: Name, age: Age)

object Person {
  lazy val personSerializer = AvroSchema[Person]
}