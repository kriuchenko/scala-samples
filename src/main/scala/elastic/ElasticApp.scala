package elastic

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.fields.TextField
import com.sksamuel.elastic4s.http.JavaClient
import com.sksamuel.elastic4s.requests.common.RefreshPolicy
import com.sksamuel.elastic4s.requests.indexes.CreateIndexRequest
import com.sksamuel.elastic4s.{ElasticClient, ElasticProperties}

import scala.concurrent.Future
//import com.sksamuel.elastic4s.playjson._
import io.circe.generic.auto._
import com.sksamuel.elastic4s.circe._

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt


case class Artist(name: String)

object ElasticApp extends App{
  val ArtistIndex = "artists"
  val elasticHost = {sys.env.getOrElse("ES_HOST", "127.0.0.1")}
  private val elasticPort = sys.env.getOrElse("ES_PORT", "9200")
  val connectionString = s"http://$elasticHost:$elasticPort"
  println(s"connectionString=$connectionString")
  val elasticProps = ElasticProperties(connectionString)
  val client = ElasticClient(JavaClient(elasticProps))
  val deleteRequest = deleteIndex(ArtistIndex)
  val createRequest: CreateIndexRequest = createIndex(ArtistIndex)
    .mapping(
      properties(
        TextField("name")
      )
    )
  val addRequest = indexInto(ArtistIndex).fields("name" -> "L.S. Lowry")
  val addCaseClassRequest = indexInto(ArtistIndex).doc(Artist(name="Sasha Lowry"))
  val findRequest = search(ArtistIndex).query("lowry")

  val json = client.show(findRequest)
  println(json)

  val eventualResponse: Future[IndexedSeq[Artist]] = for {
    _ <- client.execute(deleteRequest).recover{case _ => ()}
    _ <- client.execute(createRequest)
    _ <- client.execute(addRequest.refresh(RefreshPolicy.Immediate))
    _ <- client.execute(addCaseClassRequest.refresh(RefreshPolicy.Immediate))
    searchResponse <- client.execute(findRequest)
  } yield searchResponse.result.to[Artist]

  val response = Await.result(eventualResponse, 5.seconds)
  println(response)
  client.close()
}
