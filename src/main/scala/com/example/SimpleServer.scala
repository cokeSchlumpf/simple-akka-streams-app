package com.example

import akka.http.scaladsl.server.{ HttpApp, Route }

class SimpleServer(stream: StreamSample) extends HttpApp {

  override protected def routes: Route = get {
    onSuccess(stream.stream()) { _ => complete("ok") }
  }

}
