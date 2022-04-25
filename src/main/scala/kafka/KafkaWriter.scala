package kafka

import org.apache.kafka.clients.producer.{Callback, KafkaProducer, ProducerRecord, RecordMetadata}

import scala.concurrent.{Future, Promise}

class KafkaWriter[K, V](producer: KafkaProducer[K, V], topic: String){
  def send(message: V): Future[RecordMetadata] = {
    val record = new ProducerRecord[K, V](topic, message)
    val p = Promise[RecordMetadata]()
    val callback: Callback = (metadata: RecordMetadata, e: Exception) => if(e == null) p.success(metadata) else p.failure(e)
    producer.send(record, callback)
    p.future
  }
}