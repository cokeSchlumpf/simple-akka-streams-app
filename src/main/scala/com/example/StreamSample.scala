package com.example

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.scaladsl.{ Flow, Sink, Source }
import akka.{ Done, NotUsed }

import scala.concurrent.{ ExecutionContext, Future }
import scala.util._

case class StreamSample(implicit val system: ActorSystem, materializer: Materializer, ec: ExecutionContext) {

  def run(): Future[Done] = slickSourcePrepare
    .andThen({
      case _ => println("\n----\n")
    })
    .flatMap(_ => stream())
    .andThen({
      case Success(_) => println("Stream finished.")
      case _ =>
    })

  def stream(): Future[Done] = slickSource
    .via(httpRequest)
    // TODO mw:
    // replace ...
    .runWith(Sink.foreach(println))
  // with ..
  // .runWith(elasticsearchSink)

  def elasticsearchSink: Sink[String, NotUsed] = {
    import org.apache.http.HttpHost
    import org.elasticsearch.client.RestClient
    import akka.stream.alpakka.elasticsearch.WriteMessage
    import akka.stream.alpakka.elasticsearch.scaladsl.ElasticsearchSink
    import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
    import spray.json._

    /*
     * Domain model
     *
     * Format für JSON Dokument in ES, wird mit Spray in JSON gewandelt ...
     */
    case class Document(country: String, capital: String)

    /*
     * Dies wird benötigt um in Request/ Response in JSON HTTP Entities zu wandeln
     */
    object JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
      implicit val requestFormat: RootJsonFormat[Document] = jsonFormat2(Document)
    }

    import JsonSupport._

    /*
     * TODO mw
     * Hier die Connection URL für ES anpassen und unten den Indexname, ggf. Type name.
     */
    implicit val client: RestClient = RestClient.builder(new HttpHost("localhost", 9201)).build()

    Flow[String]
      .zipWithIndex
      .map({
        case (s, _) => WriteMessage.createIndexMessage(Document(s, s))
      })
      .to(ElasticsearchSink.create[Document]("index_name", "_type"))
  }

  def slickSourcePrepare: Future[Done] = {
    import akka.stream.alpakka.slick.scaladsl._
    import akka.stream.scaladsl._

    implicit val session: SlickSession = SlickSession.forConfig("slick-h2")
    import session.profile.api._

    /*
     * Hier fügen wir ein paar Daten in eine in-memory H2 Datenbank ein (ebenfalls mit Slick und Akka Streams) ...
     */

    Source
      .single(sqlu"CREATE TABLE FOO (FOO VARCHAR(64), BAR VARCHAR(64))")
      .runWith(Slick.sink(stmt => stmt))
      .andThen({
        case Success(_) => println("Create Table Foo ...")
        case _ =>
      })
      .flatMap(_ => Source(1 to 10)
        .map(i => sqlu"INSERT INTO FOO (FOO, BAR) VALUES ('FOO_#$i', 'BAR')")
        .runWith(Slick.sink(stmt => stmt))
        .andThen({
          case Success(_) => println("Inserted data ...")
          case _ =>
        }))
  }

  def slickSource: Source[String, NotUsed] = {
    import akka.stream.alpakka.slick.scaladsl._
    import slick.jdbc.GetResult

    /*
     * Entsprechend "slick-oracle" nutzen, wenn ihr auf die richtige Db möchtet.
     */
    implicit val session: SlickSession = SlickSession.forConfig("slick-h2")
    import session.profile.api._

    implicit val getResult: GetResult[String] = GetResult(_.nextString())

    Slick
      .source(sql"SELECT FOO FROM FOO".as[String])
  }

  /*
   * Alles was nötig ist für einen HTTP Call ...
   */
  def httpRequest: Flow[String, String, NotUsed] = {
    import akka.http.scaladsl.Http
    import akka.http.scaladsl.model._
    import akka.http.scaladsl.unmarshalling.Unmarshal
    import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
    import spray.json._

    /*
     * TODO mw
     *
     * Um einen anderen REST Service aufzurufen, sollten die Klassen Request und Response angepasst werden,
     * sowie die uri im Reques weiter unten.
     *
     * Ggf. muss das jsonFormat angepasst werden wenn die Request/ Response-Klassen geändert werden. Einfach die Zahl
     * nach jsonFormat an die Anzahl der Parameter anpassen.
     */

    /*
     * Domain model, jup - In Scala kann man Klassen innerhalb von Methoden definieren ;)
     *
     * Jetzt wird das Fromat für diese TEST API verwendet: https://jsonplaceholder.typicode.com/
     */
    case class Request(userId: Int, title: String, body: String)
    case class Response(userId: Int, id: Int, title: String, body: String)

    /*
     * Dies wird benötigt um in Request/ Response in JSON HTTP Entities zu wandeln
     */
    object JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
      implicit val requestFormat: RootJsonFormat[Request] = jsonFormat3(Request)
      implicit val responseFormat: RootJsonFormat[Response] = jsonFormat4(Response)
    }

    import JsonSupport._

    /*
      * Der eigentliche Flow zum Aufruf eine WebServices, kann man auch kürzer/ eleganter schreiben, aber so mal
      * Schritt für Schritt:
      */
    Flow[String]
      .map(s => Request.apply(4242, s, s).toJson.prettyPrint)
      .map(s => HttpEntity.apply(ContentTypes.`application/json`, s))
      .map(e => HttpRequest.apply(method = HttpMethods.POST, uri = "https://jsonplaceholder.typicode.com/posts", entity = e))
      .mapAsync(4)(request => Http().singleRequest(request))
      .map(_.entity)
      .mapAsync(4)(entity => Unmarshal(entity).to[String])
      .map(_.parseJson.convertTo[Response].title)
  }

}
