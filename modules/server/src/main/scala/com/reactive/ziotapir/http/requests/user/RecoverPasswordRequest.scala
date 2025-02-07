package com.reactive.ziotapir.http.requests.user

import zio.json.JsonCodec

final case class RecoverPasswordRequest(
  email: String,
  token: String,
  newPassword: String
) derives JsonCodec