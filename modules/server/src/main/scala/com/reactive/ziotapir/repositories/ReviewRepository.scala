package com.reactive.ziotapir.repositories

import com.reactive.ziotapir.domain.data.Review
import com.reactive.ziotapir.domain.errors.NotFoundError
import io.getquill.SnakeCase
import io.getquill.jdbczio.Quill
import zio.*
import io.getquill.*

trait ReviewRepository {
  def create(review: Review): Task[Review]
  def getById(id: Long): Task[Option[Review]]
  def getByAsin(asin: String): Task[Option[Review]]
  def getByUserId(userId: Long): Task[List[Review]]
  def getByUserExternalId(userExternalId: String): Task[List[Review]]
  def update(id: Long, op: Review => Review): Task[Review]
  def delete(id: Long): Task[Review]
  def getAll: Task[List[Review]]
}

class ReviewRepositoryLive private (quill: Quill.Postgres[SnakeCase]) extends ReviewRepository {
  import quill.*
  inline given schema: SchemaMeta[Review] = schemaMeta[Review]("reviews")
  inline given insMeta: InsertMeta[Review] = insertMeta[Review](_.id)
  inline given upMeta: UpdateMeta[Review] = updateMeta[Review](_.id)

  override def create(review: Review): Task[Review] = transaction {
    run {
      query[Review]
        .insertValue(lift(review))
        .returning(c => c)
    }
  }

  override def getById(id: Long): Task[Option[Review]] = run {
    query[Review]
      .filter(_.id == lift(id))
  }.map(_.headOption)

  override def getByAsin(asin: String): Task[Option[Review]] = run {
    query[Review]
      .filter(_.asin == lift(asin))
  }.map(_.headOption)

  override def getByUserId(userId: Long): Task[List[Review]] = run {
    query[Review]
      .filter(_.userId.exists(_ == lift(userId)))
  }

  override def getByUserExternalId(userExternalId: String): Task[List[Review]] = run {
    query[Review]
      .filter(_.userExternalId.exists(_ == lift(userExternalId)))
  }


  def update(id: Long, op: Review => Review): Task[Review] = for {
    current <- getById(id).someOrFail(NotFoundError(s"Could not update, missing id $id"))
    updated <- run {
      query[Review]
        .filter(_.id == lift(id))
        .updateValue(lift(op(current)))
        .returning(c => c)
    }
  } yield updated

  override def delete(id: Long): Task[Review] = for {
    current <- getById(id).someOrFail(NotFoundError(s"Could not delete, missing id $id"))
    deleted <- run {
      query[Review]
        .filter(_.id == lift(id))
        .delete
        .returning(c => c)
    }
  } yield deleted

  override def getAll: Task[List[Review]] = run(query[Review])
}

object ReviewRepositoryLive {
  val layer: ZLayer[Quill.Postgres[SnakeCase], Nothing, ReviewRepositoryLive] = ZLayer {
    ZIO.service[Quill.Postgres[SnakeCase]].map(q => new ReviewRepositoryLive(q))
  }
}
