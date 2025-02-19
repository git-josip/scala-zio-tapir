package com.reactive.ziotapir.http.controllers

import com.reactive.ziotapir.domain.data.{Product, User, UserId, UserToken}
import com.reactive.ziotapir.http.requests.product.CreateProductRequest
import com.reactive.ziotapir.services.{JWTService, ProductService}
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

import java.time.Instant

object ProductControllerSpec extends ZIOSpecDefault {
  private given zioME: MonadError[Task] = new RIOMonadError[Any]


  val productAsin = "B00YQ6X8EO"
  private val dummyProduct = Product(
    id = 1L,
    productAsin,
    name = "Test Product",
    url = "test.com",
    images = List("image1", "image2"),
    created = Instant.now(),
    updated = Instant.now()
  )

  private val serviceStub = new ProductService {
    override def create(createCompanyRequest: CreateProductRequest): Task[Product] = ZIO.succeed(dummyProduct)
    override def getAll: Task[List[Product]] = ZIO.succeed(List(dummyProduct))
    override def getById(id: Long): Task[Option[Product]] = ZIO.succeed(Option.when(id == dummyProduct.id)(dummyProduct))
    override def getByAsin(asin: String): Task[Option[Product]] = ZIO.succeed(Option.when(asin == dummyProduct.asin)(dummyProduct))
  }

  private val jwtServiceStub = new JWTService {
    override def createToken(user: User): Task[UserToken] = ZIO.succeed(UserToken(user.email, "bigAccess", 86400))
    override def verifyToken(token: String): Task[UserId] = ZIO.succeed(UserId(1L, "valid@user.com"))
  }

  private def backendStubZIO(endpointFun: ProductController => ServerEndpoint[Any, Task]) = for {
    controller <- ProductController.makeZio
    backendStub <- ZIO.succeed(
      TapirStubInterpreter(SttpBackendStub(MonadError[Task]))
        .whenServerEndpointRunLogic(endpointFun(controller))
        .backend()
    )
  } yield backendStub

  override def spec: Spec[TestEnvironment & Scope, Any] =
    suite("ProductControllerSpec")(
      test("post product") {
        val program = for {
          backendStub <- backendStubZIO(_.create)
          response <- basicRequest
            .post(uri"/products")
            .header("Authorization", "Bearer all_good")
            .body(
              CreateProductRequest(
                name = "Test Product",
                url = "test.com"
              ).toJson
            ).send(backendStub)
        } yield response.body

        program.assert("product create") { resBody =>
          resBody.toOption.flatMap(_.fromJson[Product].toOption)
            .contains(dummyProduct)
        }
      },

      test("get all") {
        val program = for {
          backendStub <- backendStubZIO(_.getAll)
          response <- basicRequest
            .get(uri"/products")
            .send(backendStub)
        } yield response.body

        program.assert("get all should") { resBody =>
          resBody.toOption.flatMap(_.fromJson[List[Product]].toOption)
            .contains(List(dummyProduct))
        }
      },

      test("get by id") {
        val program = for {
          backendStub <- backendStubZIO(_.getById)
          response <- basicRequest
            .get(uri"/products/1")
            .send(backendStub)
        } yield response.body

        program.assert("get by id should contain") { resBody =>
          resBody.toOption.flatMap(_.fromJson[Product].toOption)
            .contains(dummyProduct)
        }
      }
    ).provide(ZLayer.succeed(serviceStub), ZLayer.succeed(jwtServiceStub))
  end spec
}
