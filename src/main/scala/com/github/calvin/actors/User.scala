package com.github.calvin.actors

import akka.actor.{Actor, ActorLogging}
import akka.cluster.sharding.ShardRegion
import akka.pattern.{CircuitBreaker, CircuitBreakerOpenException, pipe}
import com.github.calvin.actors.User.{AddGated, AddLimited, AddNumbers, NumbersAdded}
import com.github.calvin.tools.RateLimiter.RateLimitExceeded
import com.github.calvin.tools.RateLimiter

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

class User extends Actor with ActorLogging {
  val breaker = CircuitBreaker(context.system.scheduler, maxFailures = 5, callTimeout = 5.seconds, resetTimeout = 10.seconds)
  val limiter = new RateLimiter(requests = 10, 10.seconds)
  implicit val ec: ExecutionContext = context.dispatcher

  override def receive: Receive = {
    case AddNumbers(a, b) =>
      log.info(s"Adding numbers: {}, {}", a, b)
      val theSender = sender()
      breaker.withCircuitBreaker {
        limiter.call {
          Future.successful(NumbersAdded(a, b, a + b)) pipeTo theSender
        }
      }.recover {
        case RateLimitExceeded => Future.failed(AddLimited) pipeTo theSender
        case _: CircuitBreakerOpenException => Future.failed(AddGated) pipeTo theSender
      }

    case e =>
      log.error("Unknown message received {}", e)
  }
}

object User {
  sealed trait Command
  case class AddNumbers(a: Int, b: Int) extends Command

  sealed trait Event
  case class NumbersAdded(a: Int, b: Int, result: Int) extends Event
  case object AddLimited extends Exception
  case object AddGated extends Exception

  object Sharding {
    case class EntityEnvelope(id: String, command: Command)

    val shardName: String = "user-shard"

    val extractEntityId: ShardRegion.ExtractEntityId = {
      case EntityEnvelope(id, payload) ⇒ (id.toString, payload)
    }

    def shardIdExtractor(numberOfShards: Int): ShardRegion.ExtractShardId = {
      case env: EntityEnvelope => (env.id.hashCode % numberOfShards).toString
    }
  }
}
