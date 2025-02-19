package com.reactive.ziotapir.repositories

import com.reactive.ziotapir.domain.data.Product
import com.reactive.ziotapir.domain.errors.NotFoundError
import io.getquill.*
import io.getquill.jdbczio.Quill
import zio.*

trait ProductRepository {
  def create(company: Product): Task[Product]
  def update(id: Long, op: Product => Product): Task[Product]
  def delete(id: Long): Task[Product]
  def getById(id: Long): Task[Option[Product]]
  def getByAsin(asin: String): Task[Option[Product]]
  def getAll: Task[List[Product]]
}

class ProductRepositoryLive private(quill: Quill.Postgres[SnakeCase]) extends ProductRepository {
  import quill.*
  inline given schema: SchemaMeta[Product] = schemaMeta[Product]("products")
  inline given insMeta: InsertMeta[Product] = insertMeta[Product](_.id)
  inline given upMeta: UpdateMeta[Product] = updateMeta[Product](_.id)

  override def create(company: Product): Task[Product] =
    run {
      query[Product]
        .insertValue(lift(company))
        .returning(c => c)
    }

  override def getById(id: Long): Task[Option[Product]] = run {
    query[Product]
      .filter(_.id == lift(id))
  }.map(_.headOption)

  override def getByAsin(asin: String): Task[Option[Product]] = run {
    query[Product]
      .filter(_.asin == lift(asin))
  }.map(_.headOption)

  override def getAll: Task[List[Product]] = run(query[Product])

  override def update(id: Long, op: Product => Product): Task[Product] = for {
    current <- getById(id).someOrFail(NotFoundError(s"Could not update, missing id $id"))
    updated <- run {
      query[Product]
        .filter(_.id == lift(id))
        .updateValue(lift(op(current)))
        .returning(c => c)
    }
  } yield updated

  override def delete(id: Long): Task[Product] = for {
    current <- getById(id).someOrFail(NotFoundError(s"Could not delete, missing id $id"))
    deleted <- run {
      query[Product]
        .filter(_.id == lift(id))
        .delete
        .returning(c => c)
    }
  } yield deleted
}

object ProductRepositoryLive {
  def layer: ZLayer[Quill.Postgres[SnakeCase], Nothing, ProductRepositoryLive] = ZLayer {
    ZIO.service[Quill.Postgres[SnakeCase]].map(q => ProductRepositoryLive(q))
  }
}