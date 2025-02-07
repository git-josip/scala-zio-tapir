package com.reactive.ziotapir.http.endpoints

import com.reactive.ziotapir.domain.data.Review
import com.reactive.ziotapir.http.requests.review.CreateReviewRequest
import sttp.tapir.*
import sttp.tapir.json.zio.*
import sttp.tapir.generic.auto._

trait ReviewEndpoints extends Endpoints {
  val createEndpoint: SecureEP[CreateReviewRequest, Review] = secureEndpoint
    .tag("reviews")
    .name("create")
    .description("Creates a review for a company")
    .in("reviews")
    .post
    .in(jsonBody[CreateReviewRequest])
    .out(jsonBody[Review])

  val getAllEndpoint: EP[Unit, List[Review]] = baseEndpoint
    .tag("reviews")
    .name("getAll")
    .description("Get all reviews")
    .in("reviews")
    .get
    .out(jsonBody[List[Review]])

  val getByIdEndpoint: EP[Long, Option[Review]] = baseEndpoint
    .tag("reviews")
    .name("getById")
    .description("Get a review by id")
    .in("reviews" / path[Long]("id"))
    .get
    .out(jsonBody[Option[Review]])

  val getByCompanyIdEndpoint: EP[Long, List[Review]] = baseEndpoint
    .tag("reviews")
    .name("getByCompanyId")
    .description("Get all reviews for a company")
    .in("reviews" / "company" / path[Long]("companyId"))
    .get
    .out(jsonBody[List[Review]])

  val getByUserIdEndpoint: EP[Long, List[Review]] = baseEndpoint
      .tag("reviews")
      .name("getByUserId")
      .description("get reviews written by a user")
      .in("reviews" / "user" / path[Long]("id"))
      .get
      .out(jsonBody[List[Review]])
}
