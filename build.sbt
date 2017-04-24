name := "rate-limiting-gating-per-client"

version := "1.0"

scalaVersion := "2.12.2"

libraryDependencies ++= {
  val akka = "com.typesafe.akka"
  val akkaV = "2.5.0"
  val akkaHttpV = "10.0.5"

  Seq(
    akka %% "akka-http"             % akkaHttpV,
    akka %% "akka-http-spray-json"  % akkaHttpV,
    akka %% "akka-actor"            % akkaV,
    akka %% "akka-cluster-sharding" % akkaV
  )
}
