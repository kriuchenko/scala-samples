package kafka

import akka.actor.ActorSystem
import akka.kafka.CommitterSettings
import akka.kafka.scaladsl.Committer
import akka.stream.Materializer
import akka.stream.scaladsl.Sink
import org.apache.kafka.clients.producer.{KafkaProducer, RecordMetadata}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}


object KafkaApp extends App {
  val producer = new KafkaProducer[String, String](KafkaSettings.props)
  val writer = new KafkaWriter(producer, KafkaSettings.topic)

  implicit val actorSystem = ActorSystem.create("KafkaToWebSocket")
  implicit val materializer = Materializer(actorSystem)
  val committerSettings = CommitterSettings(actorSystem)
  val reader = new KafkaReader(Set(KafkaSettings.topic), KafkaSettings.group)

  val eventualResults: Seq[Future[RecordMetadata]] = for (i <- 0 to 15) yield {
    writer.send(s"Text message nubmer $i")
  }

  val eventualResult = Future.sequence(eventualResults)
  val result = Await.result(eventualResult, 10.seconds)
  println("Producing...")
  println(result.map(m => s"${m.topic()}, ${m.partition()}, ${m.offset()}, ${m.timestamp()} \n"))
  producer.close()
  println("Consuming...")

  reader
    .topicSource
    .map{cm =>
      println(s"${cm.record.topic()}: ${cm.record.offset} = ${cm.record.value()}")
      cm.committableOffset
    }
    .via(Committer.flow(committerSettings.withMaxBatch(1)))
    .runWith(Sink.ignore)
}


