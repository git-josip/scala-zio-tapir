package com.reactive.ziotapir

import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.zio.jsonBody
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.ziohttp.{ZioHttpInterpreter, ZioHttpServerOptions}
import zio.*
import zio.http.Server

import scala.collection.mutable

object TapirDemo extends ZIOAppDefault:
  val simplestEndpoint = endpoint
    .tag("simple")
    .name("simple")
    .description("simplest endpoint possible")
    // ^^ for documentation
    .get                    // method
    .in("simple")           // path
    .out(plainBody[String]) // output
    .serverLogicSuccess[Task](_ => ZIO.succeed("All good!"))

  val simpleApp = ZioHttpInterpreter(ZioHttpServerOptions.default)
    .toHttp(simplestEndpoint)

  val simpleServerProgram = Server.serve(simpleApp)

  // simulate a job board db
  val db: mutable.Map[Long, Job] = mutable.Map(
    1L -> Job(1L, "Instructor", "rockthejvm.com", "Rock the JVM")
  )

  // create
  val createEndpoint: ServerEndpoint[Any, Task] = endpoint
    .tag("jobs")
    .name("create")
    .description("create a job")
    .in("jobs")
    .post
    .in(jsonBody[CreateJobRequest])
    .out(jsonBody[Job])
    .serverLogicSuccess[Task](req =>
      ZIO.succeed:
        val newId  = db.keys.max + 1
        val newJob = Job(newId, req.title, req.url, req.company)
        db += (newId -> newJob)
        newJob
    )

  // get by id
  val getByIdEndpoint: ServerEndpoint[Any, Task] = endpoint
    .tag("jobs")
    .name("getById")
    .description("get job by id")
    .in("jobs" / path[Long]("id"))
    .get
    .out(jsonBody[Option[Job]])
    .serverLogicSuccess[Task](id => ZIO.succeed(db.get(id)))

  // get all
  val getAllEndpoint: ServerEndpoint[Any, Task] = endpoint
    .tag("jobs")
    .name("getAll")
    .description("Get all jobs")
    .in("jobs")
    .get
    .out(jsonBody[List[Job]])
    .serverLogicSuccess[Task](_ => ZIO.succeed(db.values.toList))

  val biggerApp = ZioHttpInterpreter(ZioHttpServerOptions.default)
    .toHttp(createEndpoint :: getByIdEndpoint :: getAllEndpoint :: Nil)

  val serverProgram = Server.serve(biggerApp)

  override def run = serverProgram.provide(
    Server.default // without other configs should start at 0.0.0.0:8080
  )
end TapirDemo