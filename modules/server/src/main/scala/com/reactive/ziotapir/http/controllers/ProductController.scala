package com.reactive.ziotapir.http.controllers

import com.reactive.ziotapir.domain.data.UserId
import com.reactive.ziotapir.http.endpoints.{ProductEndpoints, HealthEndpoint}
import com.reactive.ziotapir.services.{ProductService, JWTService}
import sttp.tapir.server.ServerEndpoint
import zio.{Task, ZIO}

class ProductController private(service: ProductService, jwtService: JWTService) extends BaseController with ProductEndpoints {
  val create = createEndpoint
    .serverSecurityLogic[UserId, Task](jwtService.verifyToken(_).either)
    .serverLogic { userId => req =>
      service.create(req).either
    }

  val getAll: ServerEndpoint[Any, Task] = getAllEndpoint.serverLogic { _ => service.getAll.either }

  val getById: ServerEndpoint[Any, Task] = getByIdEndpoint.serverLogic { id =>
    ZIO
      .succeed(id.toLongOption)
      .flatMap:
        case Some(value) => service.getById(value).either
        case None        => service.getByAsin(id).either
  }

  override val routes: List[ServerEndpoint[Any, Task]] = List(
    create,
    getAll,
    getById
  )
}

object ProductController {
  val makeZio = for {
    service <- ZIO.service[ProductService]
    jwtService <- ZIO.service[JWTService]
  } yield new ProductController(service, jwtService)
}