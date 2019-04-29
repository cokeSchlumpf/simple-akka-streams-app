package com.example

import akka.Done
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}

import scala.concurrent.ExecutionContext

object SimpleApplication extends App {

  implicit val system: ActorSystem = ActorSystem.create()
  implicit val materializer: Materializer = ActorMaterializer()
  implicit val ec: ExecutionContext = system.getDispatcher

  StreamSample
    .apply()
    .run()
    .recover({
      case ex =>
        ex.printStackTrace()
        Done
    })
    .flatMap(_ => system.terminate())
    .andThen({
      case _ => println("Done.")
    })
}
