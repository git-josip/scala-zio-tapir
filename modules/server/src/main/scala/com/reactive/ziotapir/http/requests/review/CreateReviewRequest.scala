package com.reactive.ziotapir.http.requests.review

import com.reactive.ziotapir.domain.data.Review
import zio.json.JsonCodec
import java.time.Instant
import com.reactive.ziotapir.utils.generateASIN

final case class CreateReviewRequest(
  title: String,
  review: String,
  helpful: Int,
  images: Option[List[String]] = None
) derives JsonCodec:
  def toReview(userId: Long): Review =
    Review(
      id = -1L,
      asin = generateASIN(),
      title = title,
      review = review,
      helpful = helpful,
      images = images.getOrElse(List()),
      created = Instant.now(),
      updated = Instant.now()
    )
