# The REST of the Owl

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

In `zhttp`, `Request`s are processed into `Response`s via implementations of a
`sealed trait Http[-R, +E, -A, +B]`, which itself
`extends (A => ZIO[R, Option[E], B])`. From the latter, we can quickly infer
that `R` and `E` are the _resource_ and _Error_ channels of a `ZIO` effect, and
we're going to be converting an `A` to a `B` effectually.

There are some included type alias to shorten this signature, however in this
article we will still with the full version.

```scala
type HttpApp[-R, +E] = Http[R, E, Request, Response]
type UHttpApp        = HttpApp[Any, Nothing]
type RHttpApp[-R]    = HttpApp[R, Throwable]
type UHttp[-A, +B]   = Http[Any, Nothing, A, B]
```

As a quick note, this section will have `R` as `Any` and `E` as `Nothing`. We
will discuss including resources, and error handling later in the article.

Let's take a moment to dig into our first endpoint:

```scala
  val app: Http[Any, Nothing, Request, Response] = Http.collect[Request] {
    case Method.GET -> !! / "owls" => Response.text("Hoot!")
  }
```

We will use `A` and `B` here, knowing that above `A = Request` and
`B = Response`. `Http.collect[A]` is a `PartialCollect[A]` - which behaves like
a PartialFunction, meaning we're going to pattern match on something relating to
`A` and return a `B`.

We're matching against a `Request`, so let's look closer at the `case` statement
above. The tricky syntax is the infixed `->` operator, so let's first look to
the immediate _right_ of it: `!! / "owls"`. This is a `Path`, and `!!` denotes
the root of the path (an empty path, which will be important later). On the
_left_ of `->` is `Method.GET` - a `Method`. What `->` does, is tuple2 together
the things on the left/right of it. The definition is

```scala
@inline def -> [B](y: B): (A, B) = (self, y)
```

In our case

```scala
case Method.GET -> !! / "owls" => Response.text("Hoot!")
```

and

```scala
case (Method.GET, !! / "owls") => Response.text("Hoot!")
```

should behave identically. So what's going on, is we are looking at a `Request`
value, and matching on it's `Method` and `Path` - if they match, we will return
our `Response`.

It will be important for later, but we can reference the request `req` in our
response, for example, via something like

```scala
case req @ Method.GET -> !! / "owls" => Response.text("Hoot!")
```

As a quick aside, `Http.collect` also internally lifts these to `Option`s, so it
can handle the case of `None` when nothing may match.

A `Method` models an HTTP request method, i.e. `GET`, `POST`, `DELETE`, etc...

A `Path` models an HTTP request path. As mentioned above, `!!` represents the a
path root. `/` is a path delimiter that starts extraction of the left-hand side.

`/:` is a path delimiter that starts extraction of the right-hand side , and can
partially match paths. We should be careful here, as if we don't terminate the
path from the right-hand side, it could have unintended consequences. For
example, we could parse a `name` for an owl as

```scala
case Method.GET -> "owls" /: name  => Response.text(s"$name says: Hoot!")
```

and if we took to `curl`:

```shell
➜ the-rest-of-the-owl (main) ✗ curl http://localhost:9000/owls
Hoot!%
➜ the-rest-of-the-owl (main) ✗ curl http://localhost:9000/owls/Hooty
/Hooty says: Hoot!%
➜ the-rest-of-the-owl (main) ✗ curl http://localhost:9000/owls/Hooty/The/Owl
/Hooty/The/Owl says: Hoot!%
```

We can see in the third case, we're parsing more than just one segment
representing a name! To look for the appropriate number of segments, we put the
`!!` at the end to get it to behave as we hoped:

```scala
case Method.GET -> "owls" /: name /: !! => Response.text(s"$name says: Hoot!")
```

```shell
➜ the-rest-of-the-owl (main) ✗ curl http://localhost:9000/owls
Hoot!%
➜ the-rest-of-the-owl (main) ✗ curl http://localhost:9000/owls/Hooty
Hooty says: Hoot!%
➜ the-rest-of-the-owl (main) ✗ curl http://localhost:9000/owls/Hooty/The/Owl
<!DOCTYPE html><html><head><title>ZIO Http - NotFound</title><style>
 body {
   font-family: monospace;
   font-size: 16px;
   background-color: #edede0;
 }
</style></head><body><div style="margin:auto;padding:2em 4em;max-width:80%"><h1>NotFound</h1><div><div style="text-align:center"><div style="font-size:20em">404</div><div>The requested URI "/owls/Hooty/The/Owl" was not found on this server
</div></div><div><div></div></div></div></div></body></html>%
```

As a further note, you can't use `/` and `/:` in the same case statement, as
left- and right-associative operators with same precedence may not be mixed.
This also means `!!` can only be used on the left, or right, respectively.

#### Composing many Http[-R, +E, -A, +B]

In our example, we also have:

```scala
  val zApp: Http[Any, Nothing, Request, Response] =
    Http.collectZIO[Request] { case Method.POST -> !! / "owls" =>
      Random.nextIntBetween(3, 6).map(n => Response.text("Hoot! " * n))
    }
```

Note that `Http.collectZIO[Request]` behaves just like `Http.collect[Request]`,
except here instead of returning a `Response`, we'll return a
`ZIO[R, E, Response]`. Being ZIO users, it would make sense to see this form
heavily in an app that relies on our _resourceful_ logic. In the example above,
this endpoint will use the built-in `zio.Random` (which no longer needs to be
declared in the `R` channel, as we're using ZIO 2), and `Hoot` at us 3 to 5
times randomly, per request.

We then combine `app` and `zApp` to pass to the server:

```scala
  val combined: Http[Any, Nothing, Request, Response] = app ++ zApp
```

There are four operators to compose these "HTTP applications": `++`, `<>`, `>>>`
and `<<<`, and the behavior of each is as described from the
[official documentation](https://dream11.github.io/zio-http/docs/v1.x/dsl/http#composition-of-http-applications).

> ++ is an alias for defaultWith. While using ++, if the first HTTP application
> returns None the second HTTP application will be evaluated, ignoring the
> result from the first. If the first HTTP application is failing with a Some[E]
> the second HTTP application won't be evaluated.

> <> is an alias for orElse. While using <>, if the first HTTP application fails
> with Some[E], the second HTTP application will be evaluated, ignoring the
> result from the first. If the first HTTP application returns None, the second
> HTTP application won't be evaluated.

> `>>>` is an alias for andThen. It runs the first HTTP application and pipes
> the output into the other.

> <<< is the alias for compose. Compose is similar to andThen. It runs the
> second HTTP application and pipes the output to the first HTTP application.

Later in the article, we will show an example of composing these wih differing
`R` and `E` types.

### Server

At this pont, we have everything needed to start up an instance of our web
server:

```scala
  val program: ZIO[Any, Throwable, ExitCode] = for {
    _ <- Console.printLine(s"Starting server on http://localhost:$port")
    _ <- Server.start(port, combined)
  } yield ExitCode.success

  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] =
    program
```

This is a simple entry point, and we only need to give `Server.start` a port
(defined as `9000` above), and our _composed_ `Http[R, E, Request, Response]`.

Note that `Server.start` internally calls `ZIO.never`, and will block your
for-comprehension at that point. You should include it last, or append
`.forkDaemon`, and provide your own logic afterwards.

You can apply some configuration to the `Server` instance, however we won't
cover this in much capacity in this article. If interested, you can see the
official documentation
[here](https://dream11.github.io/zio-http/docs/v1.x/dsl/server/) and
[here](https://dream11.github.io/zio-http/docs/v1.x/examples/advanced-examples/advanced_server).

## Next Steps

... lift'n me higher

### Middleware

Broadly, the definition of _middleware_ is context dependent; in our realm of
our discussion, if we're turning a `Request` into a `Response`, then it's
anything we do _in the middle_ of that process. It may be something behind the
scenes, like adding logging through the process, or even more intrusive like
modifying the request to add headers to the request. Middleware is great at
helping de-couple/re-use business logic. I use the word _intrusive_ to help
emphasize that de-couple doesn't mean make _optional/not required_. For example,
if we adding logging, we probably wouldn't expect that to have an impact on the
functionality of our application. If we're parsing out information from a
header, and then adding a custom, internal authorization header, then our
application might stop working if we don't include it.

Specificity, in the context of `zhttp`, a `Middleware` is a transformation
function that converts _one_ `Http` to _another_.

```scala
type Middleware[R, E, AIn, BIn, AOut, BOut] = Http[R, E, AIn, BIn] => Http[R, E, AOut, BOut]
```

We _attach_ middleware to our `Http` via the `@@` operator. For example, we
could update our logic to use a built-in debug `Middleware` like so:

```scala
  val wrapped: Http[Console with Clock, IOException, Request, Response] =
    combined @@ Middleware.debug
```

and then, when running our application, we would see some debug messaging
printed when a client interacts with our server:

```shell
[info] Starting server on http://localhost:9000
[info] 200 GET /owls 9ms
```

In the next section, we will build our own Logging Middleware, and then looks at
a few of the other built in ones.

#### Logging

Our custom middleware is going to log some information about the `Request`
received, and the `Response` about to be sent back. We'll set up a new object
`Verbose` and define a method `log` that returns a `new Middleware`, in which we
will define the trait's `apply` method.

```scala
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
```

We're not modifying the input/output, so the types remain the same and their
values un-altered. We use `.contramapZIO` to accesses the `Request`, print some
information about it, and then return the un-altered value. We then do the same
thing with `mapZIO` for the `Response`.

This is a very simple example, but is very illustrative of how you can easily
update the `Request`/`Response` values if desired, or even fail-fast if a
particular header is missing, or unverified.

We attach our custom `Middleware` jsut as before:

```scala
val wrapped: Http[Any,Throwable,Request,Response] =
    combined @@ Verbose.log
```

and if we run our server, and make a request via curl, in our console we should
see something like:

```shell
[info] Starting server on http://localhost:9001
[info] > POST /owls Http_1_1
[info] > Host: localhost:9001
[info] > User-Agent: curl/7.79.1
[info] > Accept: */*
[info] < Ok
[info] < content-type: text/plain
```

#### Cors

To use the built-in cors, you need to instantiate a `CorsConfig` such as:

```scala
val config: CorsConfig =
    CorsConfig(
      anyOrigin = false,
      allowedOrigins = s => s.equals("localhost:9001")
    )
```

and provide it as an argument via `@@ Middleware.cors(config)`.

Note that the case class has a lot of default values provided, and it may be
unintuitive at first:

```scala
object Cors {
  final case class CorsConfig(
    anyOrigin: Boolean = true,
    anyMethod: Boolean = true,
    allowCredentials: Boolean = true,
    allowedOrigins: String => Boolean = _ => false,
    allowedMethods: Option[Set[Method]] = None,
    allowedHeaders: Option[Set[String]] = Some(
      Set(HttpHeaderNames.CONTENT_TYPE.toString, HttpHeaderNames.AUTHORIZATION.toString, "*"),
    ),
    exposedHeaders: Option[Set[String]] = Some(Set("*")),
  )
}
```

For example, you might start with
`CorsConfig(allowedOrigins = _ == "myhost.com")`, but the `anyOrigin` value is
defaulted to `true`.

**Note, that although easy to set up and apply, I have not been able to
successfully use the default implementation.**

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
