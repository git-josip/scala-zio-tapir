package com.reactive.ziotapir.http.endpoints

import com.reactive.ziotapir.domain.errors.HttpError
import sttp.tapir.Endpoint
import sttp.tapir.*

trait Endpoints {
  protected type EP[I, O] = Endpoint[Unit, I, Throwable, O, Any]
  protected type SecureEP[I, O] = Endpoint[String, I, Throwable, O, Any]

  val baseEndpoint: Endpoint[Unit, Unit, Throwable, Unit, Any] =
    endpoint
      .errorOut(statusCode and plainBody[String])
      .mapErrorOut[Throwable](HttpError.decode)(HttpError.encode)
}
