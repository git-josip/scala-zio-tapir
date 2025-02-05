package com.reactive.ziotapir.services

import com.reactive.ziotapir.domain.data.Company
import com.reactive.ziotapir.http.requests.company.CreateCompanyRequest
import com.reactive.ziotapir.repositories.CompanyRepository
import zio.*

import collection.mutable

trait CompanyService {
  def create(createCompanyRequest: CreateCompanyRequest): Task[Company]
  def getAll: Task[List[Company]]
  def getById(id: Long): Task[Option[Company]]
  def getBySlug(slug: String): Task[Option[Company]]
}

class CompanyServiceLive private (repository: CompanyRepository) extends CompanyService {
  override def create(createCompanyRequest: CreateCompanyRequest): Task[Company] = repository.create(createCompanyRequest.toCompany)

  override def getAll: Task[List[Company]] = repository.getAll

  override def getById(id: Long): Task[Option[Company]] = repository.getById(id)

  override def getBySlug(slug: String): Task[Option[Company]] = repository.getBySlug(slug)
}

object CompanyServiceLive {
  private def create(repository: CompanyRepository) = new CompanyServiceLive(repository)

  val layer = ZLayer.fromFunction(create _)
}
