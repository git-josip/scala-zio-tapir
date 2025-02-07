package com.reactive.ziotapir.http.controllers

import com.reactive.ziotapir.domain.data.{Review, User, UserId, UserToken}
import com.reactive.ziotapir.http.requests.review.CreateReviewRequest
import com.reactive.ziotapir.services.{JWTService, ReviewService}
import sttp.client3.testing.SttpBackendStub
import sttp.monad.MonadError
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.stub.TapirStubInterpreter
import sttp.tapir.ztapir.RIOMonadError
import zio.*
import zio.test.*
import sttp.client3.*
import java.time.Instant
import zio.json.*

object ReviewControllerSpec extends ZIOSpecDefault {
  private given zioME: MonadError[Task] = new RIOMonadError[Any]

  private val goodReview =
    Review(1L, 1L, 1L, 5, 5, 5, 5, 10, "all good", Instant.now(), Instant.now())

  private val serviceStub = new ReviewService:
        override def create(req: CreateReviewRequest, userId: Long): Task[Review] =
          ZIO.succeed(goodReview)
        override def getAll: Task[List[Review]] =
          ZIO.succeed(List(goodReview))
        override def getById(id: Long): Task[Option[Review]] = ZIO.succeed {
          if id == goodReview.id then Some(goodReview) else None
        }
        override def getByCompanyId(id: Long): Task[List[Review]] = ZIO.succeed {
          if id == goodReview.companyId then List(goodReview) else List.empty
        }
        override def getByUserId(id: Long): Task[List[Review]] = ZIO.succeed {
          if id == goodReview.userId then List(goodReview) else List.empty
        }
        override def delete(id: Long): Task[Review] = ZIO.succeed {
          if id == goodReview.id then goodReview else throw new RuntimeException("not found")
        }

  private val jwtServiceStub = new JWTService {
    override def createToken(user: User): Task[UserToken] = ZIO.succeed(UserToken(user.email, "bigAccess", 86400))
    override def verifyToken(token: String): Task[UserId] = ZIO.succeed(UserId(1L, "valid@user.com"))
  }

  private def backendStubZIO(endpointFun: ReviewController => ServerEndpoint[Any, Task]) = for {
    controller <- ReviewController.makeZio
    backendStub <- ZIO.succeed(
      TapirStubInterpreter(SttpBackendStub(MonadError[Task]))
        .whenServerEndpointRunLogic(endpointFun(controller))
        .backend()
    )
  } yield backendStub

  override def spec: Spec[TestEnvironment with Scope, Any] =
    suite("ReviewControllerSpec")(
      test("create") {
        for
          stub <- backendStubZIO(_.create)
          request <- ZIO.succeed {
            basicRequest
              .post(uri"/reviews")
              .header("Authorization", "Bearer all_good")
              .body(CreateReviewRequest(1L, 5, 5, 5, 5, 10, "all good").toJson)
          }
          response <- request.send(stub)
        yield assertTrue {
          response.body.toOption
            .flatMap(_.fromJson[Review].toOption)
            .contains(goodReview)
        }
      },
      test("get all") {
        for
          stub <- backendStubZIO(_.getAll)
          request <- ZIO.succeed(basicRequest.get(uri"/reviews"))
          response <- request.send(stub)
        yield assertTrue {
          response.body.toOption
            .flatMap(_.fromJson[List[Review]].toOption)
            .exists(_.contains(goodReview))
        }
      },
      test("get by id") {
        for
          stub <- backendStubZIO(_.getById)
          request <- ZIO.succeed(basicRequest.get(uri"/reviews/1"))
          response <- request.send(stub)
        yield assertTrue {
          response.body.toOption.flatMap(_.fromJson[Review].toOption)
            .contains(goodReview)
        }
      },
      test("get by company id") {
        for
          stub <- backendStubZIO(_.getByCompanyId)
          request <- ZIO.succeed(basicRequest.get(uri"/reviews/company/1"))
          response <- request.send(stub)
        yield assertTrue {
          response.body.toOption.flatMap(_.fromJson[List[Review]].toOption)
            .exists(_.contains(goodReview))
        }
      },
      test("get by user id") {
        for
          stub <- backendStubZIO(_.getByUserId)
          request <- ZIO.succeed(basicRequest.get(uri"/reviews/user/1"))
          response <- request.send(stub)
        yield assertTrue {
          response.body.toOption.flatMap(_.fromJson[List[Review]].toOption)
            .exists(_.contains(goodReview))
        }
      }
    ).provide(ZLayer.succeed(serviceStub), ZLayer.succeed(jwtServiceStub))
  end spec
}
