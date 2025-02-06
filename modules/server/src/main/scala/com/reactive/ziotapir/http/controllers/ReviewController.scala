package com.reactive.ziotapir.http.controllers

import com.reactive.ziotapir.http.endpoints.ReviewEndpoints
import com.reactive.ziotapir.services.ReviewService
import sttp.tapir.server.ServerEndpoint
import zio.{Task, ZIO}

class ReviewController private (service: ReviewService) extends BaseController with ReviewEndpoints {
  val create: ServerEndpoint[Any, Task] = createEndpoint.serverLogic { req =>
    service.create(req._2, req._1).either
  }

  val getAll: ServerEndpoint[Any, Task] = getAllEndpoint.serverLogic { _ => service.getAll.either }

  val getById: ServerEndpoint[Any, Task] = getByIdEndpoint.serverLogic { id =>
    ZIO
      .attempt(id)
      .flatMap(id => service.getById(id).either)
  }

  val getByCompanyId: ServerEndpoint[Any, Task] = getByCompanyIdEndpoint.serverLogic { service.getByCompanyId(_).either }
  val getByUserId: ServerEndpoint[Any, Task] = getByUserIdEndpoint.serverLogic { service.getByUserId(_).either }

  override val routes: List[ServerEndpoint[Any, Task]] = List(
    create,
    getAll,
    getById,
    getByCompanyId,
    getByUserId
  )
}

object ReviewController {
  val makeZio = for {
    service <- ZIO.service[ReviewService]
  } yield new ReviewController(service)
}