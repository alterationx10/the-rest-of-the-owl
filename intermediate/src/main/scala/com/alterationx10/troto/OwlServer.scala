package com.alterationx10.troto

import zhttp.http._
import zhttp.service.Server
import zio._
import java.io.IOException

object OwlServer extends ZIOAppDefault {

  val port: Int = 9000

  val app: Http[Any, Nothing, Request, Response] = Http.collect[Request] {
    case Method.GET -> !! / "owls"          => Response.text("Hoot!")
    case Method.GET -> "owls" /: name /: !! =>
      Response.text(s"$name says: Hoot!")
  }

  val zApp: Http[Any, Nothing, Request, Response] =
    Http.collectZIO[Request] { case Method.POST -> !! / "owls" =>
      Random.nextIntBetween(3, 6).map(n => Response.text("Hoot! " * n))
    }

  val combined: Http[Any, Nothing, Request, Response] = app ++ zApp

  val wrapped: Http[Console with Clock, IOException, Request, Response] =
    combined @@ Middleware.debug

  val program: ZIO[Console with Clock, Throwable, ExitCode] = for {
    _ <- Console.printLine(s"Starting server on http://localhost:$port")
    _ <- Server.start(port, wrapped)
  } yield ExitCode.success

  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] =
    program.provideSomeLayer(Clock.live ++ Console.live)
}
