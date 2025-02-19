package com.reactive.ziotapir.http.endpoints

import com.reactive.ziotapir.domain.data.Product
import com.reactive.ziotapir.http.requests.product.CreateProductRequest
import sttp.tapir.*
import sttp.tapir.json.zio.*
import sttp.tapir.generic.auto._

trait ProductEndpoints extends Endpoints {
  val createEndpoint: SecureEP[CreateProductRequest, Product] = secureEndpoint
    .tag("products")
    .name("create")
    .description("Creates a listing for a product")
    .in("products")
    .post
    .in(jsonBody[CreateProductRequest])
    .out(jsonBody[Product])

  val getAllEndpoint: EP[Unit, List[Product]] = baseEndpoint
    .tag("products")
    .name("getAll")
    .description("Get all products")
    .in("products")
    .get
    .out(jsonBody[List[Product]])

  val getByIdEndpoint: EP[String, Option[Product]] = baseEndpoint
    .tag("products")
    .name("getById")
    .description("Get a product by id")
    .in("products" / path[String]("id"))
    .get
    .out(jsonBody[Option[Product]])
}
