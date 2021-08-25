package kafkastuff

object Utils {
  sealed trait OperationType extends Product with Serializable
  final case class UserName(userName: String) extends AnyVal
  final case class Repository(repository: String) extends AnyVal
  final case class SchemaRegistryUrl(
                                      schemaRegistryUrl: String
                                    ) extends AnyVal

  final case class Password(
                             password: String
                           ) extends AnyVal

  final case class Topic(
                          topic: String
                        ) extends AnyVal
  final case class BootstrapServer(
                                    bootstrapServer: String
                                  ) extends AnyVal
  final case class GroupId(
                            groupId: String
                          ) extends AnyVal

  final case object NewSubscription                   extends OperationType
  final case object DeleteSubscription                extends OperationType
  final case class Organization(organization: String) extends AnyVal

  final case class MessageEvent(
                                 operationType: OperationType,
                                 organization: Organization,
                                 repository: Repository
                               )
}
