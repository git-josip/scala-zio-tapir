package com.reactive.ziotapir

import com.reactive.ziotapir.config.{Configs, EmailServiceConfig, JwtConfig, RecoveryTokensConfig}
import com.reactive.ziotapir.http.HttpApi
import com.reactive.ziotapir.repositories.{ProductRepositoryLive, RecoveryTokensRepositoryLive, Repository, ReviewRepositoryLive, UserRepositoryLive}
import com.reactive.ziotapir.services.{ProductServiceLive, EmailServiceLive, JWTServiceLive, ReviewServiceLive, UserServiceLive}
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
      // configs
      Configs.makeLayer[JwtConfig]("ziotapir.jwt"),
      Configs.makeLayer[RecoveryTokensConfig]("ziotapir.recoverytokens"),
      Configs.makeLayer[EmailServiceConfig]("ziotapir.email"),
      // services
      ProductServiceLive.layer,
      ReviewServiceLive.layer,
      JWTServiceLive.layer,
      UserServiceLive.layer,
      EmailServiceLive.layer,
      // repositories
      ProductRepositoryLive.layer,
      ReviewRepositoryLive.layer,
      UserRepositoryLive.layer,
      RecoveryTokensRepositoryLive.layer,
      // other
      Repository.dataLayer
    )
}
