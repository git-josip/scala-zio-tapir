package com.reactive.ziotapir

import com.reactive.ziotapir.http.HttpApi
import com.reactive.ziotapir.repositories.{CompanyRepositoryLive, Repository, ReviewRepositoryLive}
import com.reactive.ziotapir.services.{CompanyServiceLive, ReviewServiceLive}
import io.getquill.SnakeCase
import sttp.tapir.*
import sttp.tapir.server.ziohttp.{ZioHttpInterpreter, ZioHttpServerOptions}
import zio.*
import zio.http.Server

object Application extends ZIOAppDefault {
  val serverProgram = for {
    endpointsZIO <- HttpApi.endpointsZIO
    server <- Server.serve(
        ZioHttpInterpreter(
          ZioHttpServerOptions.default
        ).toHttp(endpointsZIO)
    )
    _ <- Console.printLine("Started server on port 8080")
  } yield ()


  override def run =
    serverProgram.provide(
      Server.default,
      // services
      CompanyServiceLive.layer,
      ReviewServiceLive.layer,

      // repositories
      CompanyRepositoryLive.layer,
      ReviewRepositoryLive.layer,

      // other
      Repository.dataLayer
    )
}
