package com.reactive.ziotapir.repositories

import org.testcontainers.containers.PostgreSQLContainer
import com.reactive.ziotapir.domain.data.Company
import zio.*
import zio.test.*
import com.reactive.ziotapir.syntax.*
import org.postgresql.ds.PGSimpleDataSource

import java.sql.SQLException
import javax.sql.DataSource

object CompanyRepositorySpec extends ZIOSpecDefault {
  override def spec: Spec[TestEnvironment with Scope, Any] =
    suite("CompanyRepositorySpec")(
      test("create") {
        val program = for {
          repository <- ZIO.service[CompanyRepository]
          company <- repository.create(dummyCompany)
        } yield company

        program.assert("company created") {
          case Company(_, dummyCompany.slug, dummyCompany.name, dummyCompany.url, _, _, _, _, _) => true
          case _ => false
        }
      },

      test("create duplicate - error") {
        val program = for
          repo <- ZIO.service[CompanyRepository]
          company <- repo.create(dummyCompany)
          err <- repo.create(company).flip
        yield err

        program.assert("exception assertion")(_.isInstanceOf[SQLException])
      },

      test("getById - bad id"):
        val program = for
          repo    <- ZIO.service[CompanyRepository]
          company <- repo.getById(-1L)
        yield company

        program.assert("should be empty")(_.isEmpty),

      test("getById - good id"):
        val program = for
          repo    <- ZIO.service[CompanyRepository]
          company <- repo.create(dummyCompany)
          gotten  <- repo.getById(company.id)
        yield (company, gotten)

        program.assert("should exist"):
          case (c, Some(g)) => c == g
          case _ => false,

      test("getBySlug - bad slug"):
        val program = for
          repo    <- ZIO.service[CompanyRepository]
          company <- repo.getBySlug("bogus")
        yield company

        program.assert("should be empty")(_.isEmpty),

      test("getBySlug - good slug"):
        val program = for
          repo    <- ZIO.service[CompanyRepository]
          company <- repo.create(dummyCompany)
          gotten  <- repo.getBySlug(company.slug)
        yield (company, gotten)

        program.assert("should exist"):
          case (c, Some(g)) => c == g
          case _ => false,

      test("getAll - empty"):
        val program = for
          repo      <- ZIO.service[CompanyRepository]
          companies <- repo.getAll
        yield companies

        program.assert("should be empty")(_.isEmpty),

      test("getAll - full"):
        val program = for
          repo   <- ZIO.service[CompanyRepository]
          comp1  <- repo.create(dummyCompany)
          comp2  <- repo.create(Company(-1L, "clock-the-jvm", "Clock the JVM", "clockthejvm.com"))
          gotten <- repo.getAll
        yield (comp1, comp2, gotten)

        program.assert("should contain both companies"):
          case (c1, c2, gotten) => gotten.toSet == Set(c1, c2),

      test("update record - bad id"):
        val program = for
          repo <- ZIO.service[CompanyRepository]
          err  <- repo.update(10L, _.copy(slug = "boo")).flip
        yield err

        program.assert("could not update missing id")(_.getMessage.contains("Could not update, missing id")),

      test("update record - valid change"):
        val program = for
          repo    <- ZIO.service[CompanyRepository]
          company <- repo.create(dummyCompany)
          updated <- repo.update(company.id, _.copy(url = "updated.test.com"))
          fetched <- repo.getById(company.id)
        yield (updated, fetched)

        program.assert("should be updated"):
          case (u, Some(f)) => u == f
          case _ => false,

      test("delete record - bad id"):
        val program = for
          repo <- ZIO.service[CompanyRepository]
          err  <- repo.delete(99L).flip
        yield err

        program.assert("error on invalid id")(_.getMessage.contains("Could not delete, missing id")),

      test("delete record - valid id"):
        val program = for
          repo    <- ZIO.service[CompanyRepository]
          initialCompanies <- repo.getAll
          company <- repo.create(dummyCompany)
          _       <- repo.delete(company.id)
          fetched <- repo.getById(company.id)
        yield (initialCompanies, fetched)

        program.assert("initial and final state should be empty")(res => res._1.isEmpty && res._2.isEmpty),
    ).provide(
      CompanyRepositoryLive.layer,
      dataSourceLayer,
      Repository.quillLayer,
      Scope.default
    )
  end spec

  private val dummyCompany = Company(
    id = -1,
    name = "Test Company",
    slug = "test-company",
    url = "test.com"
  )
  def createContainer(): Task[PostgreSQLContainer[Nothing]] = for {
    container <- ZIO.attempt[PostgreSQLContainer[Nothing]](PostgreSQLContainer("postgres").withInitScript("sql/companies.sql"))
    _ <- ZIO.attempt(container.start())
  } yield container

  def closeContainer(container: PostgreSQLContainer[Nothing]): UIO[Unit] =
    ZIO.attempt(container.stop()).ignoreLogged

  def createDataSource(container: PostgreSQLContainer[Nothing]): Task[DataSource] =
    ZIO.attempt:
      val dataSource = new PGSimpleDataSource()
      dataSource.setUrl(container.getJdbcUrl)
      dataSource.setUser(container.getUsername)
      dataSource.setPassword(container.getPassword)
      dataSource

  val dataSourceLayer: ZLayer[Any with Scope, Throwable, DataSource] = ZLayer:
    for
      container  <- ZIO.acquireRelease(createContainer())(closeContainer)
      dataSource <- createDataSource(container)
    yield dataSource
}
