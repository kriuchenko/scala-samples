package kafka

import java.util.Properties

object KafkaSettings{
  val props: Properties = {
    val result = new Properties()
    result.put("bootstrap.servers", "localhost:29092")
    result.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
    result.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")
    result.put("acks", "all")
    result
  }

  val topic: String = "text_topic"
  val group: String = "test"
}