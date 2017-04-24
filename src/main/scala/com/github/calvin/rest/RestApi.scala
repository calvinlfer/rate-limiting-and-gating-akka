package com.github.calvin.rest

import akka.actor.ActorRef
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.StatusCodes.TooManyRequests
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.util.Timeout
import com.github.calvin.actors.User.{AddGated, AddLimited, AddNumbers, NumbersAdded}
import com.github.calvin.actors.User.Sharding.EntityEnvelope
import com.github.calvin.models._

import scala.concurrent.Future
import scala.util.{Failure, Success}

trait RestApi extends JsonSupport {
  val userShardingRegion: ActorRef
  implicit val timeout: Timeout

  private def sendMessage(id: String, a: Int, b: Int): Future[NumbersAdded] =
    (userShardingRegion ? EntityEnvelope(id, AddNumbers(a, b))).mapTo[NumbersAdded]

  val routes: Route = path("add") {
    post {
      entity(as[Add]) { addCommand =>
        val result = sendMessage(addCommand.id, addCommand.a, addCommand.b)
        onComplete(result) {
          case Success(numbersAdded) => complete(AddResult(numbersAdded.result))
          case Failure(AddLimited) => complete(TooManyRequests, ErrorMessage("limited"))
          case Failure(AddGated) => complete(TooManyRequests, ErrorMessage("gated"))
          case _ => complete(StatusCodes.ServiceUnavailable, ErrorMessage("please try again later"))
        }
      }
    }
  }
}
