package com.reactive.ziotapir.domain.data

import java.time.Instant
import zio.json.JsonCodec

case class Review(
  id: Long,
  userId: Option[Long] = None,
  userExternalId: Option[String] = None,
  asin: String,
  title: String,
  review: String,
  helpful: Int,
  images: List[String] = List(),
  created: Instant,
  updated: Instant
) derives JsonCodec
