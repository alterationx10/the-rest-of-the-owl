package com.alterationx10.troto

import zhttp.http._
import zhttp.service.Server
import zhttp.socket._
import zio._
import java.io.IOException
import zio.stream.ZStream

object OwlServer extends ZIOAppDefault {

  val port: Int = 9002

  val content: String =
    "All work and no Play Framework makes Jack a dull boy\n" * 1000

  val data: Chunk[Byte] = Chunk.fromArray(content.getBytes(HTTP_CHARSET))

  val stream: Http[Any, Nothing, Request, Response] = Http.collect[Request] {
    case Method.GET -> !! / "stream" =>
      Response(
        status = Status.Ok,
        headers = Headers.contentLength(data.length.toLong),
        data = HttpData.fromStream(ZStream.fromChunk(data))
      )
  }

  val program: ZIO[Console with Clock, Throwable, ExitCode] = for {
    _ <- Console.printLine(s"Starting server on http://localhost:$port")
    _ <- Server.start(port, stream)
  } yield ExitCode.success

  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] =
    program.provideSomeLayer(Clock.live ++ Console.live)
}
