package com.reactive.ziotapir.domain.data

case class UserToken (
  email: String,
  accessToken: String,
  expires: Long
)
