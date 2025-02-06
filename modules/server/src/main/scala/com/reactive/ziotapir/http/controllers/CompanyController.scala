package com.reactive.ziotapir.http.controllers

import com.reactive.ziotapir.domain.data.Company
import com.reactive.ziotapir.http.endpoints.{CompanyEndpoints, HealthEndpoint}
import com.reactive.ziotapir.services.CompanyService
import sttp.tapir.server.ServerEndpoint
import zio.{Task, ZIO}

class CompanyController private (service: CompanyService) extends BaseController with CompanyEndpoints {
  val create: ServerEndpoint[Any, Task] = createEndpoint.serverLogicSuccess { req =>
    service.create(req)
  }

  val getAll: ServerEndpoint[Any, Task] = getAllEndpoint.serverLogicSuccess { _ => service.getAll }

  val getById: ServerEndpoint[Any, Task] = getByIdEndpoint.serverLogicSuccess { id =>
    ZIO
      .attempt(id.toLong)
      .flatMap(id => service.getById(id))
      .catchSome {
        case _: NumberFormatException => service.getBySlug(id)
      }
  }

  override val routes: List[ServerEndpoint[Any, Task]] = List(
    create,
    getAll,
    getById
  )
}

object CompanyController {
  val makeZio = for {
    service <- ZIO.service[CompanyService]
  } yield new CompanyController(service)
}