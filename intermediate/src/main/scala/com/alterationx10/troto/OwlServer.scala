package com.alterationx10.troto

import com.alterationx10.troto.middleware.Verbose
import zhttp.http._
import zhttp.http.middleware.Cors.CorsConfig
import zhttp.service.Server
import zio._

object OwlServer extends ZIOAppDefault {

  val port: Int = 9001

  val app: Http[Any, Nothing, Request, Response] = Http.collect[Request] {
    case Method.GET -> !! / "owls"          => Response.text("Hoot!")
    case Method.GET -> "owls" /: name /: !! =>
      Response.text(s"$name says: Hoot!")
  } @@ Middleware.csrfGenerate()

  val zApp: Http[Any, Nothing, Request, Response] =
    Http.collectZIO[Request] { case Method.POST -> !! / "owls" =>
      Random.nextIntBetween(3, 6).map(n => Response.text("Hoot! " * n))
    } @@ Middleware.csrfValidate()

  val authApp: Http[Any, Nothing, Request, Response] = Http.collect[Request] {
    case Method.GET -> !! / "secret" / "owls" =>
      Response.text("The password is 'Hoot!'")
  } @@ Middleware.basicAuth("hooty", "tootie")

  val combined: Http[Any, Nothing, Request, Response] = app ++ zApp ++ authApp

  val config: CorsConfig =
    CorsConfig(
      anyOrigin = false,
      anyMethod = false,
      allowedOrigins = s => s.equals("localhost:9001"),
      allowedMethods = Some(Set(Method.GET, Method.POST))
    )

  val allMiddleware = Verbose.log ++ Middleware.cors(config)

  val wrapped: Http[Any, Throwable, Request, Response] =
    combined @@ allMiddleware

  val program: ZIO[Any, Throwable, ExitCode] = for {
    _ <- Console.printLine(s"Starting server on http://localhost:$port")
    _ <- Server.start(port, wrapped)
  } yield ExitCode.success

  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] =
    program
}
