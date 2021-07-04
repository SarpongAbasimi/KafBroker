import Types._
import com.typesafe.config.ConfigFactory
import pureconfig.ConfigSource
import pureconfig.generic.auto._

case class KafkaConfig(
                        topicName: TopicName,
                        bootsStrapServer: BootsStrapServer,
                        keySerializer: KeySerializer,
                        valueSerializer: ValueSerializer,
                        bootsStrapServerUrl: BootsStrapServerUrl
                      )
object KafkaConfig {
  def conf(configResourceName: String, configPath: String) =
    ConfigSource.fromConfig(loadConfig(configResourceName, configPath)).loadOrThrow[KafkaConfig]
  private def loadConfig(configResourceName: String, configPath: String) =
    ConfigFactory.load(configResourceName).getConfig(configPath)
}
