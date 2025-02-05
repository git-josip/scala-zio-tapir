package com.reactive.ziotapir.http.controllers

import com.reactive.ziotapir.domain.data.Company
import com.reactive.ziotapir.http.requests.company.CreateCompanyRequest
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
            .body(
              CreateCompanyRequest(
                name = "Test Company",
                url = "test.com"
              ).toJson
            ).send(backendStub)
        } yield response.body

        program.assert("company create") { resBody =>
          resBody.toOption.flatMap(_.fromJson[Company].toOption)
            .contains(Company(1, "test-company", "Test Company", "test.com"))
        }
      },

      test("get all") {
        val program = for {
          backendStub <- backendStubZIO(_.getAll)
          response <- basicRequest
            .get(uri"/companies")
            .send(backendStub)
        } yield response.body

        program.assert("get all should be empty") { resBody =>
          resBody.toOption.flatMap(_.fromJson[List[Company]].toOption)
            .contains(List())
        }
      },

      test("get by id") {
        val program = for {
          backendStub <- backendStubZIO(_.getAll)
          response <- basicRequest
            .get(uri"/companies/1")
            .send(backendStub)
        } yield response.body

        program.assert("get by id should be empty") { resBody =>
          resBody.toOption.flatMap(_.fromJson[List[Company]].toOption)
            .isEmpty
        }
      }
    )
}
