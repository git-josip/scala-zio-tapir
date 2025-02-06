package com.reactive.ziotapir.http

import com.reactive.ziotapir.http.controllers.{BaseController, CompanyController, HealthController, ReviewController}

object HttpApi {
  def gatherRoutes(controllers: List[BaseController]) = controllers.flatMap(_.routes)

  def makeControllers = for {
    health <- HealthController.makeZio
    company <- CompanyController.makeZio
    review <- ReviewController.makeZio
  } yield List(health, company, review)

  val endpointsZIO = makeControllers.map(gatherRoutes)
}
