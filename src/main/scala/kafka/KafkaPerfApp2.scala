package kafka

import akka.actor.ActorSystem
import akka.kafka.CommitterSettings
import akka.kafka.scaladsl.Committer
import akka.stream.Materializer
import org.apache.kafka.clients.producer.{KafkaProducer, RecordMetadata}

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}


object KafkaPerfApp2 extends App {
  val BatchSize = 1*1000*1000 // Write: 100k/sec Read: 400k/sec
  val CommitBatchSize = 1
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

  writer.send(time.toString)

  val readTimer = new Timer
  val start = time
  var min = Long.MaxValue
  var max = Long.MinValue
  val outstanding = mutable.MutableList.empty[Tuple2[Long, Long]]
  var total = 0L
  var count = 0
  def avg = Math.round(total/count)
  def printStat = if(count > 0) println(s"count=$count, min=$min, avg=$avg, max=$max")

  val eventualReadResult = reader
    .topicSource
    .mapAsync(1){cm =>
      val delta: Long = time - cm.record.value().toLong
      if(delta > 1000) {
        outstanding+= Tuple2(time, delta)
      } else{
        count+=1
        total+=delta
        if(delta < min) min = delta
        if(delta > max) max = delta
      }

      println(s"${time - start} Offset: ${cm.record.offset}")
      val r = if(cm.record.offset % (100*1000) == 0) {
        outstanding.foreach(t => println(s"${t._1 - start} ${t._2}"))
        printStat
        Future.successful({})
//          throw new RuntimeException
      } else {
        if (cm.record.offset % 10 == 0)
          printStat
        writer.send(time.toString)
      }
      r.map(_ => cm.committableOffset)
    }
    .runWith(Committer.sink(committerSettings.withMaxBatch(CommitBatchSize)))
}


