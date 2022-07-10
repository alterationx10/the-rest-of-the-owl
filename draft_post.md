## Outline

... wut do'n?

## Set Up

This discussion will be based off of the latest ZIO HTTP code, which is an RC at
the time of this writing (early July 2022). This RC uses an earlier version of
ZIO (2.0.0-RC6), so other ZIO related dependencies have been selected to match
that.

The following dependencies are used:

```scala
val commonDependencies = Seq(
  "io.d11" %% "zhttp" % "2.0.0-RC9",
  "io.getquill" %% "quill-zio" % "4.0.0-RC2",
  "io.getquill" %% "quill-jdbc-zio" % "4.0.0-RC2",
  "dev.zio" %% "zio-json" % "0.3.0-RC8",
  "com.auth0" % "java-jwt" % "4.0.0",
  "com.h2database" % "h2" % "1.4.199"
)
```

There is [code repository](https://github.com/alterationx10/the-rest-of-the-owl)
to along with this post, if you care to take a look at the code in your IDE.

Going forward, I will reference the library as `zhttp`.

## Absolute Basics

We're going to start off by discussing some of the basic concepts of `zhttp`,
all based off of a fairly unassuming, self-contained program:

```scala
package com.alterationx10.troto

import zhttp.http._
import zhttp.service.Server
import zio._

object OwlServer extends ZIOAppDefault {

  val port: Int = 9000

  val app: Http[Any, Nothing, Request, Response] = Http.collect[Request] {
    case Method.GET -> !! / "owls" => Response.text("Hoot!")
  }

  val zApp: Http[Any, Nothing, Request, Response] =
    Http.collectZIO[Request] { case Method.POST -> !! / "owls" =>
      Random.nextIntBetween(3, 6).map(n => Response.text("Hoot! " * n))
    }

  val combined: Http[Any, Nothing, Request, Response] = app ++ zApp

  val program: ZIO[Any, Throwable, ExitCode] = for {
    _ <- Console.printLine(s"Starting server on http://localhost:$port")
    _ <- Server.start(port, combined)
  } yield ExitCode.success

  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] =
    program
}
```

### Request => Response

... would you like to build an HttpApp?

### Server

... start it up

## Next Steps

... lift'n me higher

### Middleware

... extending HttpApps

#### Logging

#### Cors

#### CSRF

#### Basic Auth

## Extra Credit

### Websockets

... ping <0> pong

#### Websocket Auth

### Streaming

... ups and downs

#### Requests

... Streaming uploads

#### Responses

... Streaming downloads

## The REST of the Owl

... some extra thoughts that are extra/adaptions of above

### A Brief introduction to Quill

#### Repositories

... make some repositories

#### Migrations

#### Users

##### Cookies

### Context via FiberRef

### Request Context

### Authorization Context

### Action Composition Lite :tm:

... play inspired
