package com.reactive.ziotapir.http.requests.company

import com.reactive.ziotapir.domain.data.Company
import zio.json.{DeriveJsonCodec, JsonCodec}

final case class CreateCompanyRequest(
    name: String,
    url: String,
    location: Option[String] = None,
    country: Option[String] = None,
    industry: Option[String] = None,
    image: Option[String] = None,
    tags: Option[List[String]] = None,
) {
  def toCompany =
    Company(-1, Company.makeSlug(name), name, url, location, country, industry, image, tags.getOrElse(List()))
}

object CreateCompanyRequest {
  given codec: JsonCodec[CreateCompanyRequest] = DeriveJsonCodec.gen[CreateCompanyRequest]
}
