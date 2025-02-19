package com.reactive.ziotapir.repositories

import com.reactive.ziotapir.domain.data.Product
import zio.*
import zio.test.*
import com.reactive.ziotapir.syntax.*

import java.sql.SQLException
import java.time.Instant
import javax.sql.DataSource

object ProductRepositorySpec extends ZIOSpecDefault with RepositorySpec {
  override val initScript: String = "sql/products.sql"

  override def spec: Spec[TestEnvironment with Scope, Any] =
    suite("ProductRepositorySpec")(
      test("create") {
        val program = for {
          repository <- ZIO.service[ProductRepository]
          product <- repository.create(dummyProduct)
        } yield product

        program.assert("product created") {
          case Product(_, dummyProduct.asin, dummyProduct.name, dummyProduct.url, _, _, _) => true
          case _ => false
        }
      },

      test("create duplicate - error") {
        val program = for
          repo <- ZIO.service[ProductRepository]
          product <- repo.create(dummyProduct)
          err <- repo.create(product).flip
        yield err

        program.assert("exception assertion")(_.isInstanceOf[SQLException])
      },

      test("getById - bad id"):
        val program = for
          repo    <- ZIO.service[ProductRepository]
          product <- repo.getById(-1L)
        yield product
        program.assert("should be empty")(_.isEmpty),

      test("getById - good id"):
        val program = for
          repo    <- ZIO.service[ProductRepository]
          product <- repo.create(dummyProduct)
          gotten  <- repo.getById(product.id)
        yield (product, gotten)
        program.assert("should exist"):
          case (c, Some(g)) => c == g
          case _ => false,

      test("getAll - empty"):
        val program = for
          repo      <- ZIO.service[ProductRepository]
          products <- repo.getAll
        yield products
        program.assert("should be empty")(_.isEmpty),

      test("getAll - full"):
        val program = for
          repo   <- ZIO.service[ProductRepository]
          product1  <- repo.create(dummyProduct)
          product2  <- repo.create(
            Product(
              id = -1,
              "B00YQ6X8E1",
              name = "Test Product 2",
              url = "test2.com",
              images = List("image3", "image4"),
              created = Instant.now(),
              updated = Instant.now()
            )
          )
          gotten <- repo.getAll
        yield (product1, product2, gotten)
        program.assert("should contain both products"):
          case (c1, c2, gotten) => gotten.toSet == Set(c1, c2),

      test("update record - bad id"):
        val program = for
          repo <- ZIO.service[ProductRepository]
          err  <- repo.update(10L, _.copy(asin = "B00YQ6X8E2")).flip
        yield err
        program.assert("could not update missing id")(_.getMessage.contains("Could not update, missing id")),

      test("update record - valid change"):
        val program = for
          repo    <- ZIO.service[ProductRepository]
          product <- repo.create(dummyProduct)
          updated <- repo.update(product.id, _.copy(url = "updated.test.com"))
          fetched <- repo.getById(product.id)
        yield (updated, fetched)

        program.assert("should be updated"):
          case (u, Some(f)) => u == f
          case _ => false,

      test("delete record - bad id"):
        val program = for
          repo <- ZIO.service[ProductRepository]
          err  <- repo.delete(99L).flip
        yield err

        program.assert("error on invalid id")(_.getMessage.contains("Could not delete, missing id")),

      test("delete record - valid id"):
        val program = for
          repo    <- ZIO.service[ProductRepository]
          initialProducts <- repo.getAll
          product <- repo.create(dummyProduct)
          _       <- repo.delete(product.id)
          fetched <- repo.getById(product.id)
        yield (initialProducts, fetched)
        program.assert("initial and final state should be empty")(res => res._1.isEmpty && res._2.isEmpty),
    ).provide(
      ProductRepositoryLive.layer,
      dataSourceLayer,
      Repository.quillLayer,
      Scope.default
    )
  end spec

  val productAsin = "B00YQ6X8EO"
  private val dummyProduct = Product(
    id = -1,
    productAsin,
    name = "Test Product",
    url = "test.com",
    images = List("image1", "image2"),
    created = Instant.now(),
    updated = Instant.now()
  )
}
