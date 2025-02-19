package com.reactive.ziotapir.repositories

import com.reactive.ziotapir.domain.data.Review
import zio.*
import zio.test.*
import java.time.Instant
import java.time.temporal.ChronoUnit

object ReviewRepositorySpec extends ZIOSpecDefault with RepositorySpec {
  override val initScript: String = "sql/reviews.sql"

  def spec: Spec[TestEnvironment with Scope, Any] =
    suite("ReviewRepositorySpec")(
      test("create") {
        for {
          repository <- ZIO.service[ReviewRepository]
          review <- repository.create(goodReview)
        } yield assertTrue(
          review.id == goodReview.id,
          review.asin == goodReview.asin,
          review.userId == goodReview.userId,
          review.title == goodReview.title,
          review.review == goodReview.review,
          review.helpful == goodReview.helpful,
          review.images == goodReview.images,
          review.created.truncatedTo(ChronoUnit.MILLIS) == goodReview.created.truncatedTo(ChronoUnit.MILLIS)
        )
      },

      test("get review(s) by ids - bad id") {
        for {
          repo <- ZIO.service[ReviewRepository]
          byId <- repo.getById(99L)
          byUserId <- repo.getByUserId(99L)
        } yield assertTrue(
          byId.isEmpty,
          byUserId.isEmpty
        )
      },
      test("get review(s) by ids - good id") {
        for {
          repo <- ZIO.service[ReviewRepository]
          good <- repo.create(goodReview)
          bad <- repo.create(badReview)
          byId <- repo.getById(good.id)
          byUserId <- repo.getByUserId(good.userId.get)
        } yield assertTrue(
          byId.contains(good),
          byUserId.contains(good),
          byUserId.contains(bad)
        )
      },
      test("get all") {
        for {
          repo <- ZIO.service[ReviewRepository]
          good <- repo.create(goodReview)
          bad <- repo.create(badReview)
          all <- repo.getAll
        } yield assertTrue(
          all.length == 2,
          all.contains(good),
          all.contains(bad)
        )
      },
      test("update") {
        for {
          repo <- ZIO.service[ReviewRepository]
          original <- repo.create(goodReview)
          updated <- repo.update(original.id, _.copy(review = "SOOO GOOD PRODUCT"))
        } yield assertTrue(
          updated.id == original.id,
          updated.asin == original.asin,
          updated.userId == original.userId,
          updated.title == original.title,
          updated.review == "SOOO GOOD PRODUCT",
          updated.helpful == original.helpful,
          updated.images == original.images,
          updated.created == original.created
        )
      },
      test("delete") {
        for {
          repo <- ZIO.service[ReviewRepository]
          review <- repo.create(goodReview)
          _ <- repo.delete(review.id)
          fetched <- repo.getById(review.id)
        } yield assertTrue(fetched.isEmpty)
      }
    ).provide(
      ReviewRepositoryLive.layer,
      dataSourceLayer,
      Repository.quillLayer,
      Scope.default
    )

  end spec

  private val goodReview =
    Review(1L, Some(10L), None, "B00YQ6X8EO", "Perfect product", "Great product and helped me a lot.", 1, List("image1", "image2"), Instant.now(), Instant.now())
  private val badReview =
    Review(2L, Some(10L), None, "B00YQ6X8E1", "Bad product", "Did not like it at all.", 0, List("image3"), Instant.now(), Instant.now())
}