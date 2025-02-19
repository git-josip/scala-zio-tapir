package com.reactive.ziotapir.http

import com.reactive.ziotapir.http.controllers.{BaseController, ProductController, HealthController, ReviewController, UserController}

object HttpApi {
  def gatherRoutes(controllers: List[BaseController]) = controllers.flatMap(_.routes)

  def makeControllers = for {
    health <- HealthController.makeZio
    company <- ProductController.makeZio
    review <- ReviewController.makeZio
    user <- UserController.makeZIO
  } yield List(health, company, review, user)

  val endpointsZIO = makeControllers.map(gatherRoutes)
}
