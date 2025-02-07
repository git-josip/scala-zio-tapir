package com.reactive.ziotapir.domain.data

import zio.json.JsonCodec

case class UserToken (
  email: String,
  accessToken: String,
  expires: Long
) derives JsonCodec
