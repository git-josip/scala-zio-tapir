package com.reactive.ziotapir.services

import com.reactive.ziotapir.domain.data.Product
import com.reactive.ziotapir.http.requests.product.CreateProductRequest
import com.reactive.ziotapir.repositories.ProductRepository
import zio.*

import collection.mutable

trait ProductService {
  def create(createProductRequest: CreateProductRequest): Task[Product]
  def getAll: Task[List[Product]]
  def getById(id: Long): Task[Option[Product]]
  def getByAsin(asin: String): Task[Option[Product]]
}

class ProductServiceLive private(repository: ProductRepository) extends ProductService {
  override def create(createProductRequest: CreateProductRequest): Task[Product] = repository.create(createProductRequest.toProduct)

  override def getAll: Task[List[Product]] = repository.getAll

  override def getById(id: Long): Task[Option[Product]] = repository.getById(id)

  override def getByAsin(asin: String): Task[Option[Product]] = repository.getByAsin(asin)
}

object ProductServiceLive {
  private def create(repository: ProductRepository) = new ProductServiceLive(repository)

  val layer = ZLayer.fromFunction(create _)
}
