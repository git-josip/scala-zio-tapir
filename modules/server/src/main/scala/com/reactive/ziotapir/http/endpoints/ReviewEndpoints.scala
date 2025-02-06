package com.reactive.ziotapir.http.endpoints

import com.reactive.ziotapir.domain.data.Review
import com.reactive.ziotapir.http.requests.review.CreateReviewRequest
import sttp.tapir.*
import sttp.tapir.json.zio.*
import sttp.tapir.generic.auto.*

trait ReviewEndpoints {
  val createEndpoint = endpoint
    .tag("reviews")
    .name("create")
    .description("Creates a review for a company")
    .in("reviews" / "user" / path[Long]("id"))
    .post
    .in(jsonBody[CreateReviewRequest])
    .out(jsonBody[Review])

  val getAllEndpoint = endpoint
    .tag("reviews")
    .name("getAll")
    .description("Get all reviews")
    .in("reviews")
    .get
    .out(jsonBody[List[Review]])

  val getByIdEndpoint = endpoint
    .tag("reviews")
    .name("getById")
    .description("Get a review by id")
    .in("reviews" / path[Long]("id"))
    .get
    .out(jsonBody[Option[Review]])

  val getByCompanyIdEndpoint = endpoint
    .tag("reviews")
    .name("getByCompanyId")
    .description("Get all reviews for a company")
    .in("reviews" / "company" / path[Long]("companyId"))
    .get
    .out(jsonBody[List[Review]])

  val getByUserIdEndpoint = endpoint
      .tag("reviews")
      .name("getByUserId")
      .description("get reviews written by a user")
      .in("reviews" / "user" / path[Long]("id"))
      .get
      .out(jsonBody[List[Review]])
}
