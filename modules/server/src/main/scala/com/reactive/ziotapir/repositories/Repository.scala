package com.reactive.ziotapir.repositories

import io.getquill.SnakeCase
import io.getquill.jdbczio.Quill

object Repository {
  def quillLayer = Quill.Postgres.fromNamingStrategy(SnakeCase)
  def quillDatasourceLayer = Quill.DataSource.fromPrefix("ziotapir.db")

  val dataLayer = quillDatasourceLayer >>> quillLayer
}
