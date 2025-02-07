package com.reactive.ziotapir.domain.data

final case class RecoveryToken(email: String, token: String, expiration: Long)
