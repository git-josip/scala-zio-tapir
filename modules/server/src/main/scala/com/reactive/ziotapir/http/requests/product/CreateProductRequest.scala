package com.reactive.ziotapir.http.requests.product

import com.reactive.ziotapir.domain.data.Product
import zio.json.{DeriveJsonCodec, JsonCodec}

import java.time.Instant
import scala.util.Random
import com.reactive.ziotapir.utils.generateASIN

final case class CreateProductRequest(
    name: String,
    url: String,
    images: Option[List[String]] = None,
) {
  def toProduct =
    Product(
      id = -1,
      asin = generateASIN(),
      name = name,
      url= url,
      images = images.getOrElse(List()),
      created = Instant.now(),
      updated = Instant.now()
    )
}

object CreateProductRequest {
  given codec: JsonCodec[CreateProductRequest] = DeriveJsonCodec.gen[CreateProductRequest]
}
