package com.reactive.ziotapir.services

import com.reactive.ziotapir.domain.data.Product
import com.reactive.ziotapir.http.requests.product.CreateProductRequest
import com.reactive.ziotapir.repositories.ProductRepository
import sttp.monad.MonadError
import sttp.tapir.ztapir.RIOMonadError
import zio.*
import zio.test.*
import zio.test.{Spec, TestEnvironment, ZIOSpecDefault}
import com.reactive.ziotapir.syntax.*

object ProductServiceSpec extends ZIOSpecDefault {
  private given zioME: MonadError[Task] = new RIOMonadError[Any]

  private val repositoryStubLayer = ZLayer.succeed(
    new ProductRepository {
      private val db = collection.mutable.Map[Long, Product]()

      override def create(product: Product): Task[Product] = ZIO.succeed {
        val nextId = db.keys.maxOption.getOrElse(0L) + 1
        val newProduct = product.copy(id = nextId)
        db += (newProduct.id -> newProduct)
        newProduct
      }
      override def update(id: Long, op: Product => Product): Task[Product] = ZIO.attempt {
        db. += (id -> op(db(id)))
        db(id)
      }
      override def delete(id: Long): Task[Product] = ZIO.attempt {
        val product = db(id)
        db -= id
        product
      }
      override def getById(id: Long): Task[Option[Product]] = ZIO.succeed(db.get(id))
      override def getByAsin(slug: String): Task[Option[Product]] = ZIO.succeed(db.values.find(_.asin == slug))
      override def getAll: Task[List[Product]] = ZIO.succeed(db.values.toList)
    }
  )

  val service = ZIO.serviceWithZIO[ProductService]

  override def spec: Spec[TestEnvironment with Scope, Any] =
    suite("ProductServiceTest") (
      test("create") {
        val productZIO = service(_.create(CreateProductRequest("Test Product", "test.com")))

        productZIO.assert("product created") { product =>
          product.name == "Test Product" && product.url == "test.com"
        }
      },

      test("get by id") {
        val productZIO = for {
          created <- service(_.create(CreateProductRequest("Test Product", "test.com")))
          product <- service(_.getById(created.id))
        } yield (created, product)

        productZIO.assert("product found by id") {
          case (productCreated, Some(productRetrieved)) => productCreated == productRetrieved
          case _ => false
        }
      },

      test("get by asin") {
        val productZIO = for {
          created <- service(_.create(CreateProductRequest("Test Asin", "test.com")))
          product <- service(_.getByAsin(created.asin))
        } yield (created, product)

        productZIO.assert("product found by slug") {
          case (productCreated, Some(productRetrieved)) => productCreated == productRetrieved
          case _ => false
        }
      },

      test("get all") {
        val productsZIO = for {
          product1 <- service(_.create(CreateProductRequest("Test Product", "test.com")))
          product2 <- service(_.create(CreateProductRequest("Test Product 2", "test2.com")))
          products <- service(_.getAll)
        } yield (product1, product2, products)

        productsZIO.assert("all products found")(result => List(result._1, result._2).toSet == result._3.toSet)
      }
    ).provide(
      ProductServiceLive.layer,
      repositoryStubLayer
    )
}
