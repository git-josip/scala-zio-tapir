package com.reactive.ziotapir.domain.data

import java.time.Instant
import zio.json.JsonCodec

case class Review(
  id: Long,
  companyId: Long,
  userId: Long,
  management: Int,
  culture: Int,
  salary: Int,
  benefits: Int,
  wouldRecommend: Int,
  review: String,
  created: Instant,
  updated: Instant
) derives JsonCodec
