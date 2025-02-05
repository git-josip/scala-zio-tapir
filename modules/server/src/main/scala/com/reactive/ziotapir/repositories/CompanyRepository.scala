package com.reactive.ziotapir.repositories

import com.reactive.ziotapir.domain.data.Company
import io.getquill.*
import io.getquill.jdbczio.Quill
import zio.*

trait CompanyRepository {
  def create(company: Company): Task[Company]
  def update(id: Long, op: Company => Company): Task[Company]
  def delete(id: Long): Task[Company]
  def getById(id: Long): Task[Option[Company]]
  def getBySlug(slug: String): Task[Option[Company]]
  def getAll: Task[List[Company]]
}

class CompanyRepositoryLive(quill: Quill.Postgres[SnakeCase]) extends CompanyRepository {
  import quill.*
  inline given schema: SchemaMeta[Company] = schemaMeta[Company]("companies")
  inline given insMeta: InsertMeta[Company] = insertMeta[Company](_.id)
  inline given upMeta: UpdateMeta[Company] = updateMeta[Company](_.id)

  override def create(company: Company): Task[Company] =
    run {
      query[Company]
        .insertValue(lift(company))
        .returning(c => c)
    }

  override def getById(id: Long): Task[Option[Company]] = run {
    query[Company]
      .filter(_.id == lift(id))
  }.map(_.headOption)

  override def getBySlug(slug: String): Task[Option[Company]] = run {
    query[Company]
      .filter(_.slug == lift(slug))
  }.map(_.headOption)

  override def getAll: Task[List[Company]] = run(query[Company])

  override def update(id: Long, op: Company => Company): Task[Company] = for {
    current <- getById(id).someOrFail(new RuntimeException(s"Could not update, missing id $id"))
    updated <- run {
      query[Company]
        .filter(_.id == lift(id))
        .updateValue(lift(op(current)))
        .returning(c => c)
    }
  } yield updated

  override def delete(id: Long): Task[Company] = for {
    current <- getById(id).someOrFail(new RuntimeException(s"Could not delete, missing id $id"))
    deleted <- run {
      query[Company]
        .filter(_.id == lift(id))
        .delete
        .returning(c => c)
    }
  } yield deleted
}

object CompanyRepositoryLive {
  def layer: ZLayer[Quill.Postgres[SnakeCase], Nothing, CompanyRepositoryLive] = ZLayer {
    ZIO.service[Quill.Postgres[SnakeCase]].map(q => CompanyRepositoryLive(q))
  }
}

object CompanyRepositoryDemo extends ZIOAppDefault {
  val program = for {
    repo <- ZIO.service[CompanyRepository]
    _ <- repo.create(Company(-1, "Amazon", "aws", "amazon.com"))
  } yield ()
  override def run =
    program
      .provide(
        CompanyRepositoryLive.layer,
        Quill.Postgres.fromNamingStrategy(SnakeCase),
        Quill.DataSource.fromPrefix("ziotapir.db")
      )
}
