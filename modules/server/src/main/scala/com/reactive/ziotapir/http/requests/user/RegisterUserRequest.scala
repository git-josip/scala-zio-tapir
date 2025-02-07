package com.reactive.ziotapir.http.requests.user

import zio.json.JsonCodec

final case class RegisterUserRequest(
  email: String,
  password: String
) derives JsonCodec