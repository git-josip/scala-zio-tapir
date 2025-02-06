package com.reactive.ziotapir.http.controllers

import com.reactive.ziotapir.http.endpoints.ReviewEndpoints
import com.reactive.ziotapir.services.ReviewService
import sttp.tapir.server.ServerEndpoint
import zio.{Task, ZIO}

class ReviewController private (service: ReviewService) extends BaseController with ReviewEndpoints {
  val create: ServerEndpoint[Any, Task] = createEndpoint.serverLogicSuccess { req =>
    service.create(req._2, req._1)
  }

  val getAll: ServerEndpoint[Any, Task] = getAllEndpoint.serverLogicSuccess { _ => service.getAll }

  val getById: ServerEndpoint[Any, Task] = getByIdEndpoint.serverLogicSuccess { id =>
    ZIO
      .attempt(id)
      .flatMap(id => service.getById(id))
  }

  val getByCompanyId: ServerEndpoint[Any, Task] = getByCompanyIdEndpoint.serverLogicSuccess { service.getByCompanyId }
  val getByUserId: ServerEndpoint[Any, Task] = getByUserIdEndpoint.serverLogicSuccess { service.getByUserId }

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