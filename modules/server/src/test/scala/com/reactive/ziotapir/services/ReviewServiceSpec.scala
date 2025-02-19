package com.reactive.ziotapir.services

import com.reactive.ziotapir.domain.data.Review
import com.reactive.ziotapir.http.requests.review.CreateReviewRequest
import com.reactive.ziotapir.repositories.ReviewRepository
import zio.*
import zio.test.*
import com.reactive.ziotapir.utils.generateASIN
import java.time.Instant

object ReviewServiceSpec extends ZIOSpecDefault {
  val repositoryStub = new ReviewRepository:
    override def create(review: Review): Task[Review] = ZIO.succeed(goodReview)
    override def getAll: Task[List[Review]] = ZIO.succeed(List.empty)
    override def getById(id: Long): Task[Option[Review]] = update(id, identity)
      .map(Option.apply)
      .catchAll(_ => ZIO.succeed(None))
    override def getByAsin(asin: String): Task[Option[Review]] =
      if asin != goodReview.asin then ZIO.fail(new RuntimeException("bad asin"))
      else ZIO.succeed(goodReview).map(Option.apply)

    override def getByUserId(userId: Long): Task[List[Review]] = ZIO.succeed {
      if goodReview.userId.contains(userId) then List(goodReview) else Nil
    }

    override def getByUserExternalId(userExternalId: String): Task[List[Review]] = ZIO.succeed {
      if goodReview.userExternalId.contains(userExternalId) then List(goodReview) else Nil
    }
    override def update(id: Long, op: Review => Review): Task[Review] =
      if id != goodReview.id then ZIO.fail(new RuntimeException("bad id"))
      else ZIO.succeed(op(goodReview))
    override def delete(id: Long): Task[Review] =
      update(id, identity)
  end repositoryStub

  override def spec: Spec[TestEnvironment with Scope, Any] =
    suite("ReviewServiceSpec")(
      test("create") {
        for {
          service <- ZIO.service[ReviewService]
          review <- service.create(createReviewRequest, 1L)
        } yield assertTrue:
          review.id == 1L &&
            review.userId.contains(2L) &&
            review.asin == poductAsin &&
            review.userExternalId.contains("ext1") &&
            review.title == createReviewRequest.title &&
            review.review == createReviewRequest.review &&
            review.helpful == createReviewRequest.helpful
      },
      test("getById") {
        for
        service <- ZIO.service[ReviewService]
        review <- service.getById (goodReview.id)
        yield assertTrue (review.contains (goodReview) )
      },
      test("getByUserId") {
        for
          service <- ZIO.service[ReviewService]
          reviews <- service.getByUserId(goodReview.userId.get)
        yield assertTrue(reviews.contains(goodReview))
      }
    ).provide(
      ReviewServiceLive.layer,
      ZLayer.succeed(repositoryStub)
    )
  end spec

  val createReviewRequest = CreateReviewRequest(
    title = "Perfect product",
    review = "Great product and helped me a lot.",
    helpful = 1,
    images = Some(List("image1", "image2")),
  )
  val poductAsin = generateASIN()
  private val goodReview =
    Review(1L, Some(2L), Some("ext1"), poductAsin, "Perfect product", "Great product and helped me a lot.", 1, List("image1", "image2"), Instant.now(), Instant.now())
}
