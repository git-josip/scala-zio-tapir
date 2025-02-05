package com.reactive.ziotapir.services

import com.reactive.ziotapir.domain.data.Company
import com.reactive.ziotapir.http.requests.company.CreateCompanyRequest
import zio.*
import collection.mutable

trait CompanyService {
  def create(createCompanyRequest: CreateCompanyRequest): Task[Company]
  def getAll: Task[List[Company]]
  def getById(id: Long): Task[Option[Company]]
  def getBySlug(slug: String): Task[Option[Company]]
}

object CompanyService {
  val dummyLayer: ZLayer[Any, Nothing, CompanyService] = ZLayer.succeed(new CompanyServiceDummy)
}

class CompanyServiceDummy extends CompanyService {
  val db = mutable.Map[Long, Company]()

  override def create(createCompanyRequest: CreateCompanyRequest): Task[Company] = ZIO.succeed {
    val newId = db.keys.maxOption.getOrElse(0L) + 1
    val newCompany = createCompanyRequest.toCompany(newId)
    db += (newId -> newCompany)
    newCompany
  }

  override def getAll: Task[List[Company]] = ZIO.succeed(db.values.toList)

  override def getById(id: Long): Task[Option[Company]] = ZIO.succeed(db.get(id))

  override def getBySlug(slug: String): Task[Option[Company]] = ZIO.succeed(db.values.find(_.slug == slug))
}
