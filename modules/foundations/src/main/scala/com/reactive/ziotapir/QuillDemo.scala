package com.reactive.ziotapir

import io.getquill.*
import io.getquill.jdbczio.Quill
import zio.*

object QuillDemo extends ZIOAppDefault:
  val program: ZIO[JobRepository, Throwable, Unit] = for
    repo <- ZIO.service[JobRepository]
    _ <- repo.create(
      Job(
        -1,
        "Software Engineer",
        "rockthejvm.com",
        "Rock the JVM"
      )
    )
  yield ()

  override def run: Task[Unit] = program.provide(
    JobRepositoryLive.layer,
    Quill.Postgres.fromNamingStrategy(SnakeCase),
    Quill.DataSource.fromPrefix("mydbconf")
  )
end QuillDemo

// repository
trait JobRepository:
  def create(job: Job): Task[Job]
  def update(id: Long, op: Job => Job): Task[Job]
  def delete(id: Long): Task[Job]
  def getById(id: Long): Task[Option[Job]]
  def get: Task[List[Job]]

class JobRepositoryLive(quill: Quill.Postgres[SnakeCase]) extends JobRepository:
  import quill.*
  inline given schema: SchemaMeta[Job] = schemaMeta[Job]("jobs")
  inline given insMeta: InsertMeta[Job] =
    insertMeta[Job](_.id) // columns to be excluded in inserts
  inline given upMeta: UpdateMeta[Job] =
    updateMeta[Job](_.id) // columns to be excluded in updates

  override def create(job: Job): Task[Job] =
    run:
      query[Job]
        .insertValue(lift(job))
        .returning(j => j)

  override def update(id: Long, op: Job => Job): Task[Job] =
    for
      current <- getById(id).someOrFail(
        new RuntimeException(s"could not update, missing key $id")
      )
      updated <- run:
        query[Job]
          .filter(_.id == lift(id))
          .updateValue(lift(op(current)))
          .returning(j => j)
    yield updated

  override def delete(id: Long): Task[Job] =
    run:
      query[Job]
        .filter(_.id == lift(id))
        .delete
        .returning(j => j)

  override def getById(id: Long): Task[Option[Job]] =
    run:
      query[Job].filter(_.id == lift(id))
    .map(_.headOption)

  override def get: Task[List[Job]] =
    run:
      query[Job]
end JobRepositoryLive

object JobRepositoryLive:
  val layer: ZLayer[Quill.Postgres[SnakeCase], Nothing, JobRepositoryLive] =
    ZLayer:
      ZIO.service[Quill.Postgres[SnakeCase]].map(q => JobRepositoryLive(q))
