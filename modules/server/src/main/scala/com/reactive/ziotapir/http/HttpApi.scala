package com.reactive.ziotapir.http

import com.reactive.ziotapir.http.controllers.{BaseController, CompanyController, HealthController}

object HttpApi {
  def gatherRoutes(controllers: List[BaseController]) = controllers.flatMap(_.routes)

  def makeControllers = for {
    health <- HealthController.makeZio
    company <- CompanyController.makeZio
  } yield List(health, company)

  val endpointsZIO = makeControllers.map(gatherRoutes)
}
