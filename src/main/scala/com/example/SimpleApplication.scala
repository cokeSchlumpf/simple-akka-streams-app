package com.example

import akka.actor.ActorSystem
import akka.stream.{ ActorMaterializer, Materializer }

import scala.concurrent.ExecutionContext

object SimpleApplication extends App {

  implicit val system: ActorSystem = ActorSystem.create()
  implicit val materializer: Materializer = ActorMaterializer()
  implicit val ec: ExecutionContext = system.getDispatcher

  val stream = StreamSample.apply()

  stream
    .slickSourcePrepare
    .map(_ => new SimpleServer(stream).startServer("0.0.0.0", 8080))

}
