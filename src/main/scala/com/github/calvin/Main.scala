package com.github.calvin

import akka.actor.{ActorSystem, Props}
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings}
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.github.calvin.actors.User
import com.github.calvin.rest.RestApi

import scala.util.{Failure, Success}
import scala.concurrent.duration._

object Main extends App with RestApi {
  implicit val system = ActorSystem("example-system")
  implicit val materializer = ActorMaterializer()
  implicit val ec = system.dispatcher
  implicit val timeout = Timeout(3.seconds)
  val config = system.settings.config

  val userShardingRegion = ClusterSharding(system).start(
    typeName = User.Sharding.shardName,
    entityProps = Props[User],
    settings = ClusterShardingSettings(system),
    extractEntityId = User.Sharding.extractEntityId,
    extractShardId = User.Sharding.shardIdExtractor(config.getInt("users.sharding.number-of-shards"))
  )

  val bindingFuture = Http().bindAndHandle(routes, "localhost", 9001)
  bindingFuture.onComplete {
    case Success(serverBinding) =>
      system.log.info(s"Bound to {}", serverBinding.localAddress)

    case Failure(error) =>
      system.log.error(error.getMessage)
      system.terminate()
  }
}
