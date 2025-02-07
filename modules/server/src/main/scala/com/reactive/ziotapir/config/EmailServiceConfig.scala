package com.reactive.ziotapir.config

final case class EmailServiceConfig(
  host: String,
  port: Int,
  user: String,
  pass: String
)

