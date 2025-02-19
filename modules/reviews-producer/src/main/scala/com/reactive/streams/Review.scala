package com.reactive.streams

import zio.json.JsonCodec

case class Review(
  rating: Double,
  title: String,
  text: String,
  asin: String,
  images: List[String],
  parent_asin: String,
  user_id: String,
  timestamp: Long,
  helpful_vote: Int,
  verified_purchase: Boolean
) derives JsonCodec

case class DeleterReview(
  line: String,
  error: String
) derives JsonCodec