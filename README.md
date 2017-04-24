# Rate limiting and gating on a per-client basis with Akka

This project is an illustration of how to perform rate limiting for a specific client by making use of actors that 
represent clients. In addition to rate limiting per client, we also show an example of how to perform gating wherein
if you keep trying to bombard the server with requests even when you get back 'limited' messages, you will become gated
and you will not be allowed to use the underlying service and all calls will continue to fail fast until the client 
decides to back off and wait for the required time. This is good practice as it ensures that you do not overwhelm your 
servers. Usually rate limiting can be done by API gateways like [Kong](https://getkong.org/plugins/rate-limiting) but 
you can achieve the same behavior by implementing it yourself and layering on more sophisticated behavior with a toolkit
like Akka. 

## Design

We use Akka Cluster Sharding to represent clients (the `User` Actor). Each `User` provides a simple service which is the
addition of 2 numbers. You can imagine that more complex business logic or contacting fragile internal systems could be
performed here. If you fire off too many requests to the sharded `User` Actor, it will limit you making the calls fail 
fast and if you continue to try and spam the `User` Actor with more requests, it will gate you preventing you from ever 
using the service until the caller stops misbehaving. This technique is taken from [Reactive Design Patterns](https://www.manning.com/books/reactive-design-patterns).

We use a [`RateLimiter`](https://github.com/ReactiveDesignPatterns/CodeSamples/blob/master/chapter12/src/main/scala/com/reactivedesignpatterns/chapter12/RateLimiter.scala) 
and [`Circuit Breaker`](http://doc.akka.io/docs/akka/current/common/circuitbreaker.html) in combination to provide the 
limiting and gating behavior. The Circuit Breaker wraps the Rate Limiter so if you exceed the call limit, the Rate 
Limiter will fail and if you exceed the Rate Limiter too many times, the Circuit Breaker will open.

```scala
  breaker.withCircuitBreaker {
    limiter.call {
      Future.successful(NumbersAdded(a, b, a + b)) pipeTo theSender
    }
  }.recover {
    case RateLimitExceeded => Future.failed(AddLimited) pipeTo theSender
    case _: CircuitBreakerOpenException => Future.failed(AddGated) pipeTo theSender
  }
```

We expose this add functionality over HTTP with JSON with the help of [Akka HTTP](http://doc.akka.io/docs/akka-http/current/).
A client makes a `POST` request with their ID as a part of the body. For example:

```curl
curl -X POST \
  http://localhost:9001/add \
  -H 'content-type: application/json' \
  -d '{
	"a": 1,
	"b": 2,
	"id": "example-client-id"
}'
```

If a client misbehaves and sends more than 10 requests in a span of 10 seconds then you will start to receive HTTP 429s 
with a `limited` message and if you continue to keep sending requests then you will become `gated` and all requests from 
that particular client will continue to fail fast until the client decides to stop misbehaving. Using another client id 
will use a separate rate limiter and that will continue to work provided your clients don't misbehave. 

### Credits: 
- https://www.manning.com/books/reactive-design-patterns
