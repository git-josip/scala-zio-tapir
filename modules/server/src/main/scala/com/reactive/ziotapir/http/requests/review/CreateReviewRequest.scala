package com.reactive.ziotapir.http.requests.review

import com.reactive.ziotapir.domain.data.Review
import zio.json.JsonCodec
import java.time.Instant

final case class CreateReviewRequest(
  companyId: Long,
  management: Int,
  culture: Int,
  salary: Int,
  benefits: Int,
  wouldRecommend: Int,
  review: String
) derives JsonCodec:
  def toReview(id: Long, userId: Long): Review =
    Review(
      id,
      companyId,
      userId,
      management,
      culture,
      salary,
      benefits,
      wouldRecommend,
      review,
      Instant.now(),
      Instant.now()
    )
