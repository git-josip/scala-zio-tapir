package com.reactive.ziotapir.domain.data

import zio.json.{DeriveJsonCodec, JsonCodec}
import java.time.Instant

case class Product(
  id: Long,
  asin: String,
  name: String,
  url: String,
  images: List[String] = List(),
  created: Instant,
  updated: Instant
)

object Product {
  given codec: JsonCodec[Product] = DeriveJsonCodec.gen[Product]
}
