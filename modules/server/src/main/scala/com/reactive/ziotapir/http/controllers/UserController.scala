package com.reactive.ziotapir.http.controllers

import com.reactive.ziotapir.domain.data.UserId
import com.reactive.ziotapir.domain.errors.UnauthorizedError
import com.reactive.ziotapir.http.endpoints.UserEndpoints
import com.reactive.ziotapir.http.responses.UserResponse
import com.reactive.ziotapir.services.{JWTService, UserService}
import sttp.tapir.*
import sttp.tapir.server.ServerEndpoint
import zio.*

class UserController private (userService: UserService, jwtService: JWTService) extends BaseController with UserEndpoints:
  val create: ServerEndpoint[Any, Task] =
    createUserEndpoint.serverLogic: req =>
      userService
        .register(req.email, req.password)
        .map(user => UserResponse(user.email))
        .either

  val login: ServerEndpoint[Any, Task] =
    loginEndpoint.serverLogic: req =>
      userService.generateToken(req.email, req.password).either

  val changePassword: ServerEndpoint[Any, Task] =
    updatePasswordEndpoint
      .serverSecurityLogic[UserId, Task](jwtService.verifyToken(_).either)
      .serverLogic: _ =>
        req =>
          userService
            .updatePassword(req.email, req.oldPassword, req.newPassword)
            .map(user => UserResponse(user.email))
            .either

  val delete: ServerEndpoint[Any, Task] =
    deleteEndpoint
      .serverSecurityLogic[UserId, Task](jwtService.verifyToken(_).either)
      .serverLogic: _ =>
        req =>
          userService
            .deleteUser(req.email, req.password)
            .map(user => UserResponse(user.email))
            .either

  val forgotPassword: ServerEndpoint[Any, Task] =
    forgotPasswordEndpoint
      .serverLogic: req =>
        userService
          .sendRecoveryToken(req.email)
          .either

  val recoverPassword: ServerEndpoint[Any, Task] =
    recoverPasswordEndpoint
      .serverLogic: req =>
        userService
          .recoverFromToken(req.email, req.token, req.newPassword)
          .filterOrFail(b => b)(UnauthorizedError("Invalid email/token combination."))
          .unit
          .either

  override val routes: List[ServerEndpoint[Any, Task]] =
    List(create, login, changePassword, delete, forgotPassword, recoverPassword)

object UserController:
  val makeZIO: URIO[JWTService with UserService, UserController] =
    for
      userService <- ZIO.service[UserService]
      jwtService  <- ZIO.service[JWTService]
    yield UserController(userService, jwtService)
