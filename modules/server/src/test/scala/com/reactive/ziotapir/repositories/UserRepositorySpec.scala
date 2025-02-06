package com.reactive.ziotapir.repositories

import com.reactive.ziotapir.domain.data.User
import zio.*
import zio.test.*

object UserRepositorySpec extends ZIOSpecDefault with RepositorySpec:
  override val initScript: String = "sql/users.sql"

  private val user = User(-1L, "test@fakemail.com", "aStrongPassword123")

  override def spec: Spec[TestEnvironment with Scope, Any] =
    suite("UserRepositorySpec")(
      test("create"):
        for
          repo    <- ZIO.service[UserRepository]
          created <- repo.create(user)
        yield assertTrue:
          created.email == user.email &&
            created.hashedPassword == user.hashedPassword
      ,
      test("getById"):
        for
          repo      <- ZIO.service[UserRepository]
          created   <- repo.create(user)
          gotten    <- repo.getById(created.id)
          notGotten <- repo.getById(-99L)
        yield assertTrue:
          gotten.contains(created) &&
            notGotten.isEmpty
      ,
      test("getByEmail"):
        for
          repo      <- ZIO.service[UserRepository]
          created   <- repo.create(user)
          gotten    <- repo.getByEmail(created.email)
          notGotten <- repo.getByEmail("invalid@mail.com")
        yield assertTrue:
          gotten.contains(created) &&
            notGotten.isEmpty
      ,
      test("update"):
        for
          repo    <- ZIO.service[UserRepository]
          created <- repo.create(user)
          updated <- repo.update(created.id, _.copy(email = "another@mail.com"))
        yield assertTrue:
          updated.id == created.id &&
            updated.email == "another@mail.com" &&
            updated.hashedPassword == created.hashedPassword
      ,
      test("delete"):
        for
          repo      <- ZIO.service[UserRepository]
          created   <- repo.create(user)
          _         <- repo.delete(created.id)
          notGotten <- repo.getById(created.id)
        yield assertTrue(notGotten.isEmpty)
    ).provide(
      UserRepositoryLive.layer,
      dataSourceLayer,
      Repository.quillLayer,
      Scope.default
    )
