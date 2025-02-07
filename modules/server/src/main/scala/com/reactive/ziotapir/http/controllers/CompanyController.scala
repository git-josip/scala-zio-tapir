package com.reactive.ziotapir.http.controllers

import com.reactive.ziotapir.domain.data.UserId
import com.reactive.ziotapir.http.endpoints.{CompanyEndpoints, HealthEndpoint}
import com.reactive.ziotapir.services.{CompanyService, JWTService}
import sttp.tapir.server.ServerEndpoint
import zio.{Task, ZIO}

class CompanyController private (service: CompanyService, jwtService: JWTService) extends BaseController with CompanyEndpoints {
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
        case None        => service.getBySlug(id).either
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
    jwtService <- ZIO.service[JWTService]
  } yield new CompanyController(service, jwtService)
}