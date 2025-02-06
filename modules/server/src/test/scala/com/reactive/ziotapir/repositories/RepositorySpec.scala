package com.reactive.ziotapir.repositories

import org.testcontainers.containers.PostgreSQLContainer
import org.postgresql.ds.PGSimpleDataSource
import zio.*
import javax.sql.DataSource

trait RepositorySpec {
  val initScript: String

  def createContainer(): Task[PostgreSQLContainer[Nothing]] =
    ZIO.attempt:
      val container: PostgreSQLContainer[Nothing] =
        PostgreSQLContainer("postgres")
          .withInitScript(initScript)
      container.start()
      container

  def closeContainer(container: PostgreSQLContainer[Nothing]): UIO[Unit] =
    ZIO.attempt(container.stop()).ignoreLogged

  def createDataSource(container: PostgreSQLContainer[Nothing]): Task[DataSource] = {
    ZIO.attempt:
      val dataSource = new PGSimpleDataSource()
      dataSource.setUrl(container.getJdbcUrl)
      dataSource.setUser(container.getUsername)
      dataSource.setPassword(container.getPassword)
      dataSource
  }

  val dataSourceLayer: ZLayer[Any with Scope, Throwable, DataSource] = {
    ZLayer:
      for
        container <- ZIO.acquireRelease(createContainer())(closeContainer)
        dataSource <- createDataSource(container)
      yield dataSource
  }
}
