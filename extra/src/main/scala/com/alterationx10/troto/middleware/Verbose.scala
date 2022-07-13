package com.alterationx10.troto.middleware

import zhttp.http._
import zio._

object Verbose {

  def log[R, E >: Throwable]
      : Middleware[R, E, Request, Response, Request, Response] =
    new Middleware[R, E, Request, Response, Request, Response] {

      override def apply[R1 <: R, E1 >: E](
          http: Http[R1, E1, Request, Response]
      ): Http[R1, E1, Request, Response] =
        http
          .contramapZIO[R1, E1, Request] { r =>
            for {
              _ <- Console.printLine(s"> ${r.method} ${r.path} ${r.version}")
              _ <- ZIO.foreach(r.headers.toList) { h =>
                     Console.printLine(s"> ${h._1}: ${h._2}")
                   }
            } yield r
          }
          .mapZIO[R1, E1, Response] { r =>
            for {
              _ <- Console.printLine(s"< ${r.status}")
              _ <- ZIO.foreach(r.headers.toList) { h =>
                     Console.printLine(s"< ${h._1}: ${h._2}")
                   }
            } yield r
          }

    }

}
