package com.reactive.ziotapir.integration

import com.reactive.ziotapir.config.{JwtConfig, RecoveryTokensConfig}
import com.reactive.ziotapir.domain.data.{RecoveryToken, UserToken}
import zio.*
import com.reactive.ziotapir.http.controllers.UserController
import com.reactive.ziotapir.http.requests.user.{
  ForgotPasswordRequest,
  LoginRequest,
  RecoverPasswordRequest,
  RegisterUserRequest,
  UpdatePasswordRequest
}
import com.reactive.ziotapir.http.responses.UserResponse
import com.reactive.ziotapir.repositories.{
  RecoveryTokensRepositoryLive,
  Repository,
  RepositorySpec,
  UserRepositoryLive
}
import com.reactive.ziotapir.services.*
import sttp.client3.*
import sttp.client3.testing.SttpBackendStub
import sttp.model.Method
import sttp.monad.MonadError
import sttp.tapir.server.stub.TapirStubInterpreter
import sttp.tapir.ztapir.RIOMonadError
import zio.test.*
import zio.json.*

object UserFlowSpec extends ZIOSpecDefault with RepositorySpec:
  override val initScript: String = "sql/integration.sql"

  private given zioMonadError: MonadError[Task] =
    new RIOMonadError[Any]

  private def backendStubZIO
  : URIO[JWTService & UserService, SttpBackend[Task, Nothing]] =
    for controller <- UserController.makeZIO
      yield TapirStubInterpreter(SttpBackendStub(MonadError[Task]))
        .whenServerEndpointsRunLogic(controller.routes)
        .backend()

  extension [A : JsonCodec](backend: SttpBackend[Task, Nothing])
    def sendReq[B : JsonCodec](method: Method, path: String, payload: A, maybeToken: Option[String] = None): Task[Option[B]] =
      basicRequest
        .method(method, uri"$path")
        .body(payload.toJson)
        .auth
        .bearer(maybeToken.getOrElse(""))
        .send(backend)
        .map(_.body.toOption.flatMap(payload => payload.fromJson[B].toOption))

    def post[B : JsonCodec](path: String, payload: A, maybeToken: Option[String] = None): Task[Option[B]] =
      sendReq(Method.POST, path, payload, maybeToken)

    def postNoResponse(path: String, payload: A): Task[Unit] =
      basicRequest
        .post(uri"$path")
        .body(payload.toJson)
        .send(backend)
        .unit

    def put[B : JsonCodec](
                            path: String,
                            payload: A,
                            maybeToken: Option[String] = None
                          ): Task[Option[B]] =
      sendReq(Method.PUT, path, payload, maybeToken)
  end extension

  private class EmailServiceProbe extends EmailService:
    private val db = collection.mutable.Map.empty[String, RecoveryToken]
    override def sendEmail(
                            to: String,
                            subject: String,
                            content: String
                          ): Task[Unit] = ZIO.unit
    override def sendPasswordRecovery(to: String, token: String): Task[Unit] =
      ZIO.succeed:
        db += (to -> RecoveryToken(to, token, -1L))
    def probe(email: String): Task[Option[String]] =
      ZIO.succeed:
        db.get(email).map(_.token)
  end EmailServiceProbe

  private val regReq = RegisterUserRequest("valid@user.com", "aPassword")
  private val logReq = LoginRequest("valid@user.com", "aPassword")
  private val pReq =
    UpdatePasswordRequest("valid@user.com", "aPassword", "newOne")

  override def spec: Spec[TestEnvironment with Scope, Any] =
    suite("UserFlowSpec")(
      test("create user"):
        for
          backend  <- backendStubZIO
          response <- backend.post[UserResponse]("/users", regReq)
        yield assertTrue:
          response.contains(UserResponse("valid@user.com"))
      ,
      test("create and login"):
        for
          backend <- backendStubZIO
          _       <- backend.post[UserResponse]("/users", regReq)
          token   <- backend.post[UserToken]("/users/login", logReq)
        yield assertTrue:
          token.isDefined &&
            token.get.email == "valid@user.com"
      ,
      test("change password"):
        for
          backend <- backendStubZIO
          _       <- backend.post[UserResponse]("/users", regReq)
          token <- backend
            .post[UserToken]("/users/login", logReq)
            .someOrFail(new RuntimeException("auth failed"))
          _ <- backend
            .put[UserResponse]("/users/password", pReq, Some(token.accessToken))
          oldTok <- backend.post[UserToken]("/users/login", logReq)
          newTok <- backend
            .post[UserToken](
              "/users/login",
              logReq.copy(password = pReq.newPassword)
            )
        yield assertTrue:
          oldTok.isEmpty &&
            newTok.isDefined &&
            newTok.get.email == "valid@user.com"
      ,
      test("recover password flow"):
        for
          backend <- backendStubZIO
          _       <- backend.post[UserResponse]("/users", regReq)
          _ <- backend.postNoResponse(
            "/users/forgot",
            ForgotPasswordRequest(regReq.email)
          )
          probe <- ZIO.service[EmailServiceProbe]
          token <- probe
            .probe(regReq.email)
            .someOrFail(new RuntimeException("token was not emailed"))
          _ <- backend.postNoResponse(
            "/users/recover",
            RecoverPasswordRequest(regReq.email, token, "newPass")
          )
          oldTok <- backend.post[UserToken]("/users/login", logReq)
          newTok <- backend
            .post[UserToken]("/users/login", logReq.copy(password = "newPass"))
        yield assertTrue:
          oldTok.isEmpty &&
            newTok.exists(_.email == regReq.email),
    ).provide(
      Scope.default,
      UserServiceLive.layer,
      JWTServiceLive.layer,
      UserRepositoryLive.layer,
      Repository.quillLayer,
      dataSourceLayer,
      ZLayer.succeed(JwtConfig("secret", 3600)),
      RecoveryTokensRepositoryLive.layer,
      ZLayer.succeed(RecoveryTokensConfig(3600L)),
      ZLayer.succeed(EmailServiceProbe())
    )

