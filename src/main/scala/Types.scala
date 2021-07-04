object Types {
  final case class Name(name: String) extends AnyVal
  final case class Age(age:Int) extends AnyVal
  final case class BootsStrapServer(bootStrapServer: String) extends AnyVal
  final case class BootsStrapServerUrl(bootsStrapServerUrl: String) extends  AnyVal
  final case class TopicName(topicName: String) extends AnyVal
  final case class KeySerializer(keySerializer: String) extends AnyVal
  final case class ValueSerializer(valueSerializer: String) extends AnyVal
}