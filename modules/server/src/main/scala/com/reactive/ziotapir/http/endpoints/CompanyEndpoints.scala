package com.reactive.ziotapir.http.endpoints

import com.reactive.ziotapir.domain.data.Company
import com.reactive.ziotapir.http.requests.company.CreateCompanyRequest
import sttp.tapir.*
import sttp.tapir.json.zio.*
import sttp.tapir.generic.auto._

trait CompanyEndpoints extends Endpoints {
  val createEndpoint: SecureEP[CreateCompanyRequest, Company] = secureEndpoint
    .tag("companies")
    .name("create")
    .description("Creates a listing for a company")
    .in("companies")
    .post
    .in(jsonBody[CreateCompanyRequest])
    .out(jsonBody[Company])

  val getAllEndpoint: EP[Unit, List[Company]] = baseEndpoint
    .tag("companies")
    .name("getAll")
    .description("Get all companies")
    .in("companies")
    .get
    .out(jsonBody[List[Company]])

  val getByIdEndpoint: EP[String, Option[Company]] = baseEndpoint
    .tag("companies")
    .name("getById")
    .description("Get a company by id")
    .in("companies" / path[String]("id"))
    .get
    .out(jsonBody[Option[Company]])
}
