package com.reactive.ziotapir.config

import com.typesafe.config.ConfigFactory
import zio.*
import zio.config.*
import zio.config.magnolia.*
import zio.config.typesafe.*

object Configs {
  def makeLayer[C: Tag](path: String)(implicit deriveConfig: DeriveConfig[C]): ZLayer[Any, Throwable, C] =
    ZLayer.fromZIO {
      for {
        rawConfig <- ZIO.attempt(ConfigFactory.load().getConfig(path))
        configProvider = ConfigProvider.fromTypesafeConfig(rawConfig)
        result <- configProvider.load(deriveConfig.desc)
      } yield result
    }
}
