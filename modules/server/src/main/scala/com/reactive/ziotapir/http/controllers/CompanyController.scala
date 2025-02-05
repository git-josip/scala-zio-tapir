package com.reactive.ziotapir.http.controllers

import com.reactive.ziotapir.domain.data.Company
import com.reactive.ziotapir.http.endpoints.{CompanyEndpoints, HealthEndpoint}
import sttp.tapir.server.ServerEndpoint
import zio.{Task, ZIO}

import collection.mutable
class CompanyController private extends BaseController with CompanyEndpoints {
  val db = mutable.Map[Long, Company](
    -1L -> Company(-1, "invalid", "Not Company", "")
  )

  val create: ServerEndpoint[Any, Task] = createEndpoint.serverLogicSuccess { req =>
    ZIO.succeed {
      val newId = db.keys.max + 1
      val newCompany = req.toCompany(newId)
      db += (newId -> newCompany)
      newCompany
    }
  }

  val getAll: ServerEndpoint[Any, Task] = getAllEndpoint.serverLogicSuccess { _ =>
    ZIO.succeed(db.values.toList)
  }

  val getById: ServerEndpoint[Any, Task] = getByIdEndpoint.serverLogicSuccess { id =>
    ZIO
      .attempt(id.toLong)
      .map(id => db.get(id))
  }

  override val routes: List[ServerEndpoint[Any, Task]] = List(
    create,
    getAll,
    getById
  )
}

object CompanyController {
  val makeZio = ZIO.succeed(new CompanyController)
}