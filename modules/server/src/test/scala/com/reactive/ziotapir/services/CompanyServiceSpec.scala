package com.reactive.ziotapir.services

import com.reactive.ziotapir.domain.data.Company
import com.reactive.ziotapir.http.requests.company.CreateCompanyRequest
import com.reactive.ziotapir.repositories.CompanyRepository
import sttp.monad.MonadError
import sttp.tapir.ztapir.RIOMonadError
import zio.*
import zio.test.*
import zio.test.{Spec, TestEnvironment, ZIOSpecDefault}
import com.reactive.ziotapir.syntax.*

object CompanyServiceSpec extends ZIOSpecDefault {
  private given zioME: MonadError[Task] = new RIOMonadError[Any]

  private val repositoryStubLayer = ZLayer.succeed(
    new CompanyRepository {
      val db = collection.mutable.Map[Long, Company]()

      override def create(company: Company): Task[Company] = ZIO.succeed {
        val nextId = db.keys.maxOption.getOrElse(0L) + 1
        val newCompany = company.copy(id = nextId)
        db += (newCompany.id -> newCompany)
        newCompany
      }
      override def update(id: Long, op: Company => Company): Task[Company] = ZIO.attempt {
        db. += (id -> op(db(id)))
        db(id)
      }
      override def delete(id: Long): Task[Company] = ZIO.attempt {
        val company = db(id)
        db -= id
        company
      }
      override def getById(id: Long): Task[Option[Company]] = ZIO.succeed(db.get(id))
      override def getBySlug(slug: String): Task[Option[Company]] = ZIO.succeed(db.values.find(_.slug == slug))
      override def getAll: Task[List[Company]] = ZIO.succeed(db.values.toList)
    }
  )

  val service = ZIO.serviceWithZIO[CompanyService]

  override def spec: Spec[TestEnvironment with Scope, Any] =
    suite("CompanyServiceTest") (
      test("create") {
        val companyZIO = service(_.create(CreateCompanyRequest("Test Company", "test.com")))

        companyZIO.assert("company created") { company =>
          company.name == "Test Company" && company.url == "test.com"
        }
      },

      test("get by id") {
        val companyZIO = for {
          created <- service(_.create(CreateCompanyRequest("Test Company", "test.com")))
          company <- service(_.getById(created.id))
        } yield (created, company)

        companyZIO.assert("company found by id") {
          case (companyCreated, Some(companyRetrieved)) => companyCreated == companyRetrieved
          case _ => false
        }
      },

      test("get by slug") {
        val companyZIO = for {
          created <- service(_.create(CreateCompanyRequest("Test Company", "test.com")))
          company <- service(_.getBySlug(created.slug))
        } yield (created, company)

        companyZIO.assert("company found by slug") {
          case (companyCreated, Some(companyRetrieved)) => companyCreated == companyRetrieved
          case _ => false
        }
      },

      test("get all") {
        val companiesZIO = for {
          company1 <- service(_.create(CreateCompanyRequest("Test Company", "test.com")))
          company2 <- service(_.create(CreateCompanyRequest("Test Company 2", "test2.com")))
          companies <- service(_.getAll)
        } yield (List(company1, company2), companies)

        companiesZIO.assert("all companies found") {
          case (companiesCreated, companiesRetrieved) => companiesCreated == companiesRetrieved
          case _ => false
        }
      }
    ).provide(
      CompanyServiceLive.layer,
      repositoryStubLayer
    )
}
