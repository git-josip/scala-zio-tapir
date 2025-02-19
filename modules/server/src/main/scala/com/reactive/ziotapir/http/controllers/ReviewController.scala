package com.reactive.ziotapir.http.controllers

import com.reactive.ziotapir.http.endpoints.ReviewEndpoints
import com.reactive.ziotapir.services.{JWTService, ReviewService}
import sttp.tapir.server.ServerEndpoint
import zio.{Task, ZIO}

class ReviewController private (service: ReviewService, jwtService: JWTService) extends BaseController with ReviewEndpoints {
  val create: ServerEndpoint[Any, Task] = createEndpoint
    .serverSecurityLogic(jwtService.verifyToken(_).either)
    .serverLogic { userId => req =>
    service.create(req, userId.id).either
  }

  val getAll: ServerEndpoint[Any, Task] = getAllEndpoint.serverLogic { _ => service.getAll.either }

  val getById: ServerEndpoint[Any, Task] = getByIdEndpoint.serverLogic { id =>
    ZIO
      .attempt(id)
      .flatMap(id => service.getById(id).either)
  }

  val getByUserId: ServerEndpoint[Any, Task] = getByUserIdEndpoint.serverLogic { service.getByUserId(_).either }

  override val routes: List[ServerEndpoint[Any, Task]] = List(
    create,
    getAll,
    getById,
    getByUserId
  )
}

object ReviewController {
  val makeZio = for {
    service <- ZIO.service[ReviewService]
    jwtService <- ZIO.service[JWTService]
  } yield new ReviewController(service, jwtService)
}