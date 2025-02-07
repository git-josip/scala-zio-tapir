package com.reactive.ziotapir.http.requests.user

import zio.json.JsonCodec

final case class ForgotPasswordRequest(email: String) derives JsonCodec