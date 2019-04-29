lazy val akkaHttpVersion = "10.1.8"
lazy val akkaVersion = "2.5.22"
lazy val alpakkaVersion = "1.0.0"

/*
 * TODO mw:
 * Hier ggf. das interne Nexus Repository hinzufügen
 */
// resolvers += "Nexus" at "<URL>"

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "com.example",
      scalaVersion := "2.12.7"
    )),
    name := "httptest",
    libraryDependencies ++= Seq(
      /*
       * TODO mw: 
       * Hier noch die Abhäbgigkeit zum Oracle Treiber hinzufügen, equivalent zum H2 Treiber.
       * Der Treiber sollte im Nexus unter den genutzen Koordinaten verfügbar sein
       */
      // "com.oracle" % "ojdbc" % "14",
      "com.h2database" % "h2" % "1.4.199",

      "com.lightbend.akka" %% "akka-stream-alpakka-elasticsearch" % alpakkaVersion,
      "com.lightbend.akka" %% "akka-stream-alpakka-slick" % alpakkaVersion,

      "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-xml" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-stream" % akkaVersion,

      "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test,
      "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,
      "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % Test,
      "org.scalatest" %% "scalatest" % "3.0.5" % Test
    ),
    mainClass in assembly := Some("com.example.SimpleApplication"))
