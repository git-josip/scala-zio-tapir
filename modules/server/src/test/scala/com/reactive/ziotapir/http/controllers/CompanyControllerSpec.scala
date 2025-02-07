package com.reactive.ziotapir.http.controllers

import com.reactive.ziotapir.domain.data.{Company, User, UserId, UserToken}
import com.reactive.ziotapir.http.requests.company.CreateCompanyRequest
import com.reactive.ziotapir.services.{CompanyService, JWTService}
import sttp.client3.testing.SttpBackendStub
import sttp.monad.MonadError
import sttp.tapir.server.stub.TapirStubInterpreter
import sttp.tapir.ztapir.RIOMonadError
import zio.json.*
import sttp.client3.*
import sttp.tapir.server.ServerEndpoint
import zio.*
import zio.test.*
import com.reactive.ziotapir.syntax.*

object CompanyControllerSpec extends ZIOSpecDefault {
  private given zioME: MonadError[Task] = new RIOMonadError[Any]

  private val dummyCompany = Company(1L, "test-company", "Test Company", "test.com")
  private val serviceStub = new CompanyService {
    override def create(createCompanyRequest: CreateCompanyRequest): Task[Company] = ZIO.succeed(dummyCompany)
    override def getAll: Task[List[Company]] = ZIO.succeed(List(dummyCompany))
    override def getById(id: Long): Task[Option[Company]] = ZIO.succeed(Option.when(id == dummyCompany.id)(dummyCompany))
    override def getBySlug(slug: String): Task[Option[Company]] = ZIO.succeed(Option.when(slug == dummyCompany.slug)(dummyCompany))
  }

  private val jwtServiceStub = new JWTService {
    override def createToken(user: User): Task[UserToken] = ZIO.succeed(UserToken(user.email, "bigAccess", 86400))
    override def verifyToken(token: String): Task[UserId] = ZIO.succeed(UserId(1L, "valid@user.com"))
  }

  private def backendStubZIO(endpointFun: CompanyController => ServerEndpoint[Any, Task]) = for {
    controller <- CompanyController.makeZio
    backendStub <- ZIO.succeed(
      TapirStubInterpreter(SttpBackendStub(MonadError[Task]))
        .whenServerEndpointRunLogic(endpointFun(controller))
        .backend()
    )
  } yield backendStub

  override def spec: Spec[TestEnvironment & Scope, Any] =
    suite("CompanyController")(
      test("post company") {
        val program = for {
          backendStub <- backendStubZIO(_.create)
          response <- basicRequest
            .post(uri"/companies")
            .header("Authorization", "Bearer all_good")
            .body(
              CreateCompanyRequest(
                name = "Test Company",
                url = "test.com"
              ).toJson
            ).send(backendStub)
        } yield response.body

        program.assert("company create") { resBody =>
          resBody.toOption.flatMap(_.fromJson[Company].toOption)
            .contains(dummyCompany)
        }
      },

      test("get all") {
        val program = for {
          backendStub <- backendStubZIO(_.getAll)
          response <- basicRequest
            .get(uri"/companies")
            .send(backendStub)
        } yield response.body

        program.assert("get all should") { resBody =>
          resBody.toOption.flatMap(_.fromJson[List[Company]].toOption)
            .contains(List(dummyCompany))
        }
      },

      test("get by id") {
        val program = for {
          backendStub <- backendStubZIO(_.getById)
          response <- basicRequest
            .get(uri"/companies/1")
            .send(backendStub)
        } yield response.body

        program.assert("get by id should contain") { resBody =>
          resBody.toOption.flatMap(_.fromJson[Company].toOption)
            .contains(dummyCompany)
        }
      }
    ).provide(ZLayer.succeed(serviceStub), ZLayer.succeed(jwtServiceStub))
  end spec
}
