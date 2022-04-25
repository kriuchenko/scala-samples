package kafka

import akka.NotUsed
import akka.actor.ActorSystem
import akka.kafka.ConsumerMessage.CommittableMessage
import akka.kafka.{ConsumerSettings, Subscriptions}
import akka.kafka.scaladsl.Consumer
import akka.stream.Materializer
import akka.stream.scaladsl.{BroadcastHub, Source}
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer

import scala.concurrent.duration.DurationInt

class KafkaReader[K, V](topics: Set[String], groupId: String)(implicit actorSystem: ActorSystem, materializer: Materializer){
  def topicSource: Source[CommittableMessage[String, String], NotUsed] = {
    val kafkaConsumerSettings = ConsumerSettings.create(
      system = actorSystem,
      keyDeserializer = new StringDeserializer,
      valueDeserializer =  new StringDeserializer
    ).withBootstrapServers(KafkaSettings.props.get("bootstrap.servers").asInstanceOf[String])
      .withGroupId(groupId)
      .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
      .withStopTimeout(5.seconds)

    Consumer
      .committableSource(kafkaConsumerSettings, Subscriptions.topics(topics))
      //      .plainSource(kafkaConsumerSettings, Subscriptions.topics(topics))
      .map(committableMsg => committableMsg)
      .runWith(BroadcastHub.sink[CommittableMessage[String, String]]) // using a broadcast hub here, ensures that all websocket clients will use the same consumer
  }
}