package com.reactive.ziotapir.services

import com.reactive.ziotapir.domain.data.Review
import com.reactive.ziotapir.http.requests.review.CreateReviewRequest
import com.reactive.ziotapir.repositories.ReviewRepository
import zio.*

trait ReviewService {
  def create(req: CreateReviewRequest, userId: Long): Task[Review]
  def getAll: Task[List[Review]]
  def getById(id: Long): Task[Option[Review]]
  def getByAsin(asin: String): Task[Option[Review]]
  def getByUserId(userId: Long): Task[List[Review]]
  def getByUserExternalIdId(userExternalId: String): Task[List[Review]]
  def delete(id: Long): Task[Review]
}

class ReviewServiceLive private (repository: ReviewRepository) extends ReviewService {
  override def create(req: CreateReviewRequest, userId: Long): Task[Review] =
    repository.create(req.toReview(userId))

  override def getAll: Task[List[Review]] = repository.getAll

  override def getById(id: Long): Task[Option[Review]] = repository.getById(id)

  override def getByAsin(asin: String): Task[Option[Review]] = repository.getByAsin(asin)

  override def getByUserId(userId: Long): Task[List[Review]] = repository.getByUserId(userId)

  override def getByUserExternalIdId(userExternalId: String): Task[List[Review]] = repository.getByUserExternalId(userExternalId)

  override def delete(id: Long): Task[Review] = repository.delete(id)
}

object ReviewServiceLive {
  val layer = ZLayer {
    for {
      repository <- ZIO.service[ReviewRepository]
    } yield new ReviewServiceLive(repository)
  }
}
