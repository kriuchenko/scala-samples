package kafka

import akka.actor.ActorSystem
import akka.kafka.CommitterSettings
import akka.kafka.scaladsl.Committer
import akka.stream.Materializer
import org.apache.kafka.clients.producer.{KafkaProducer, RecordMetadata}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}


object KafkaPerfApp extends App {
  val BatchSize = 10*1000*1000 // Write: 100k/sec Read: 400k/sec
  val CommitBatchSize = 1000*1000
  val timeout = 600

  def time: Long = System.currentTimeMillis()
  class Timer{
    val start = time
    def delta: Long = time - start
  }

  val producer = new KafkaProducer[String, String](KafkaSettings.props)
  val writer = new KafkaWriter(producer, KafkaSettings.topic)

  implicit val actorSystem = ActorSystem.create("KafkaToWebSocket")
  implicit val materializer = Materializer(actorSystem)
  val committerSettings = CommitterSettings(actorSystem)
  val reader = new KafkaReader(Set(KafkaSettings.topic), KafkaSettings.group)

  val writeTimer = new Timer
  val eventualResults: Seq[Future[RecordMetadata]] = for (i <- 1 to BatchSize) yield {
    writer.send(s"{'name': 'test', 'value': $i}")
  }

  val eventualResult = Future.sequence(eventualResults)
  val result = Await.result(eventualResult, timeout.seconds)
  println(s"Produced $BatchSize in ${writeTimer.delta}")
//  println(result.map(m => s"${m.topic()}, ${m.partition()}, ${m.offset()}, ${m.timestamp()} \n"))
  producer.close()
  println("Consuming...")

  val readTimer = new Timer
  val eventualReadResult = reader
    .topicSource
    .map{cm =>
      if(cm.record.offset % CommitBatchSize == 0) {
        println(s"Consumed $CommitBatchSize in ${readTimer.delta}")
        println(s"${cm.record.topic()}: ${cm.record.offset} = ${cm.record.value()}")
        if(cm.record.offset % BatchSize == 0)
          actorSystem.terminate()
      }
      cm.committableOffset
    }
    .runWith(Committer.sink(committerSettings.withMaxBatch(CommitBatchSize)))
}


