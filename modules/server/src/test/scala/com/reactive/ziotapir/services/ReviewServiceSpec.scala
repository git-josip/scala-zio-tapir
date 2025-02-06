package com.reactive.ziotapir.services

import com.reactive.ziotapir.domain.data.Review
import com.reactive.ziotapir.http.requests.review.CreateReviewRequest
import com.reactive.ziotapir.repositories.ReviewRepository
import zio.*
import zio.test.*

import java.time.Instant

object ReviewServiceSpec extends ZIOSpecDefault {
  val repositoryStub = new ReviewRepository:
    override def create(review: Review): Task[Review] = ZIO.succeed(goodReview)
    override def getAll: Task[List[Review]] = ZIO.succeed(List.empty)
    override def getById(id: Long): Task[Option[Review]] = update(id, identity)
      .map(Option.apply)
      .catchAll(_ => ZIO.succeed(None))
    override def getByCompanyId(companyId: Long): Task[List[Review]] = ZIO.succeed {
      if companyId == goodReview.companyId then List(goodReview) else Nil
    }
    override def getByUserId(userId: Long): Task[List[Review]] = ZIO.succeed {
      if userId == goodReview.userId then List(goodReview) else Nil
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
            review.companyId == createReviewRequest.companyId &&
            review.userId == 1L &&
            review.management == createReviewRequest.management &&
            review.culture == createReviewRequest.culture &&
            review.salary == createReviewRequest.salary &&
            review.wouldRecommend == createReviewRequest.wouldRecommend &&
            review.review == createReviewRequest.review
      },
      test("getById") {
        for
        service <- ZIO.service[ReviewService]
        review <- service.getById (goodReview.id)
        yield assertTrue (review.contains (goodReview) )
      },
      test("getByCompanyId") {
        for
          service <- ZIO.service[ReviewService]
          reviews <- service.getByCompanyId(goodReview.companyId)
        yield assertTrue(reviews.contains(goodReview))
      },
      test("getByUserId") {
        for
          service <- ZIO.service[ReviewService]
          reviews <- service.getByUserId(goodReview.userId)
        yield assertTrue(reviews.contains(goodReview))
      }
    ).provide(
      ReviewServiceLive.layer,
      ZLayer.succeed(repositoryStub)
    )
  end spec

  val createReviewRequest = CreateReviewRequest(
    companyId = 1L,
    management = 5,
    culture = 4,
    salary = 3,
    benefits = 2,
    wouldRecommend = 1,
    review = "Great company!"
  )
  private val goodReview =
    Review(1L, 1L, 1L, 5, 4, 3, 2, 1, "Great company!", Instant.now(), Instant.now())
}
