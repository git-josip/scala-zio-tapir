package com.reactive.ziotapir.services

import com.auth0.jwt.{JWT, JWTVerifier}
import com.auth0.jwt.algorithms.Algorithm
import com.reactive.ziotapir.config.{Configs, JwtConfig}
import com.reactive.ziotapir.domain.data.{User, UserId, UserToken}
import zio.*

import java.time.Instant

trait JWTService {
  def createToken(user: User): Task[UserToken]
  def verifyToken(token: String): Task[UserId]
}

class JWTServiceLive(jwtConfig: JwtConfig, clock: java.time.Clock) extends JWTService {
  private val ISSUER = "ziotapir.com"
  private val algorithm = Algorithm.HMAC512(jwtConfig.secret)
  private val CLAIM_USERNAME = "username"

  private val verifier = JWT
    .require(algorithm)
    .withIssuer(ISSUER)
    .asInstanceOf[JWTVerifier.BaseVerification]
    .build(clock)

  override def createToken(user: User): Task[UserToken] =
    for
      now        <- ZIO.attempt(clock.instant())
      expiration <- ZIO.succeed(now.plusSeconds(jwtConfig.ttl))
      token      <- ZIO.attempt(makeJwt(user, now, expiration))
    yield UserToken(user.email, token, expiration.getEpochSecond)

  override def verifyToken(token: String): Task[UserId] = {
    for
      decoded <- ZIO.attempt(verifier.verify(token))
      userId <- ZIO.attempt {
        UserId(
          decoded.getSubject.toLong,
          decoded.getClaim(CLAIM_USERNAME).asString()
        )
      }
    yield userId
  }

  private def makeJwt(user: User, now: Instant, expiration: Instant): String =
    JWT
      .create()
      .withIssuer(ISSUER)
      .withIssuedAt(now)
      .withExpiresAt(expiration)
      .withSubject(user.id.toString)
      .withClaim(CLAIM_USERNAME, user.email)
      .sign(algorithm)
}

object JWTServiceLive {
  val layer: ZLayer[JwtConfig, Nothing, JWTServiceLive] = ZLayer {
    for
      config <- ZIO.service[JwtConfig]
      clock <- Clock.javaClock
    yield JWTServiceLive(config, clock)
  }

  val configuredLayer: ZLayer[Any, Throwable, JWTServiceLive] =
    Configs.makeLayer[JwtConfig]("ziotapir.jwt") >>> layer
}
