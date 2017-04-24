package com.github.calvin.tools

import com.github.calvin.tools.RateLimiter._

import scala.concurrent.Future
import scala.concurrent.duration.{Deadline, FiniteDuration}

/**
  * Rate Limiter from Reactive Design Patterns by Roland Kuhn
  * @param requests is the number of requests allowed in the specified period
  * @param period is a finite amount of time
  */
class RateLimiter(requests: Int, period: FiniteDuration) {
  private val startTimes = {
    val onePeriodAgo = Deadline.now - period
    Array.fill(requests)(onePeriodAgo)
  }
  private var position = 0
  private def lastTime = startTimes(position)
  private def enqueue(time: Deadline) = {
    startTimes(position) = time
    position += 1
    if (position == requests) position = 0
  }
  def call[T](block: => Future[T]): Future[T] = {
    val now = Deadline.now
    if ((now - lastTime) < period) Future.failed(RateLimitExceeded)
    else {
      enqueue(now)
      block
    }
  }
}

object RateLimiter {
  case object RateLimitExceeded extends RuntimeException
}