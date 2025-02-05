package com.reactive.ziotapir.http.controllers

import com.reactive.ziotapir.http.endpoints.HealthEndpoint
import sttp.tapir.server.ServerEndpoint
import zio.{Task, ZIO}

class HealthController private extends BaseController with HealthEndpoint {
  val health: ServerEndpoint[Any, Task] = healthEndpoint
    .serverLogicSuccess[Task](_ => ZIO.succeed("All good!"))

  override val routes: List[ServerEndpoint[Any, Task]] = List(health)
}

object HealthController {
  val makeZio = ZIO.succeed(new HealthController)
}