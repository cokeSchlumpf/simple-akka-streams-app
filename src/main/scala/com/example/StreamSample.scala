package com.example

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, Source}
import akka.{Done, NotUsed}

import scala.concurrent.{ExecutionContext, Future}

case class StreamSample(implicit val system: ActorSystem, materializer: Materializer, ec: ExecutionContext) {

  def run(): Future[Done] = {
    Source(1 to 10)
      .map(i => "Text " + i)
      .via(httpRequest)
      .runForeach(println)
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
      implicit val responseFormat:RootJsonFormat[Response] = jsonFormat4(Response)
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
