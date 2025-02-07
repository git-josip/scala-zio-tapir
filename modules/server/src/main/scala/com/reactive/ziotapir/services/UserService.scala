package com.reactive.ziotapir.services

import com.reactive.ziotapir.domain.data.{User, UserToken}
import com.reactive.ziotapir.domain.errors.UnauthorizedError
import com.reactive.ziotapir.repositories.{RecoveryTokensRepository, UserRepository}
import zio.*

import java.security.{MessageDigest, SecureRandom}
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

trait UserService {
  def register(email: String, password: String): Task[User]
  def verifyPassword(email: String, password: String): Task[Boolean]
  def generateToken(email: String, password: String): Task[Option[UserToken]]
  def getById(id: Long): Task[Option[User]]
  def getByEmail(email: String): Task[Option[User]]
  def updatePassword(email: String, oldPassword: String, newPassword: String): Task[User]
  def deleteUser(email: String, password: String): Task[User]
  def sendRecoveryToken(email: String): Task[Unit]
  def recoverFromToken(email: String, token: String, newPassword: String): Task[Boolean]
}

class UserServiceLive private (
  jwtService: JWTService,
  emailService: EmailService,
  tokenRepo: RecoveryTokensRepository,
  repository: UserRepository
) extends UserService {
  override def register(email: String, password: String): Task[User] =
    repository.create(
      User(
        id = -1,
        email = email,
        hashedPassword = UserServiceLive.Hasher.generateHash(password)
      )
    )

  override def verifyPassword(email: String, password: String): Task[Boolean] =
    for {
      existingUser <- repository.getByEmail(email).someOrFail(UnauthorizedError("Invalid email or password"))
      validation <- ZIO.attempt(UserServiceLive.Hasher.validateHash(password, existingUser.hashedPassword))
        .mapError(_ => UnauthorizedError("Invalid email or password"))
    } yield validation

  override def getById(id: Long): Task[Option[User]] =
    repository.getById(id)

  override def getByEmail(email: String): Task[Option[User]] =
    repository.getByEmail(email)

  override def generateToken(email: String, password: String): Task[Option[UserToken]] =
    for {
      existingUser <- repository.getByEmail(email).someOrFail(UnauthorizedError("Invalid email or password"))
      verified <- ZIO.attempt(UserServiceLive.Hasher.validateHash(password, existingUser.hashedPassword))
        .mapError(_ => UnauthorizedError("Invalid email or password"))
      maybeToken <- jwtService.createToken(existingUser).when(verified)
    } yield maybeToken

  override def updatePassword(email: String, oldPassword: String, newPassword: String): Task[User] = {
    import UserServiceLive.Hasher.generateHash
    for
      user <- verifyUser(email, oldPassword)
      updated <- repository.update(
        user.id,
        _.copy(hashedPassword = generateHash(newPassword))
      )
    yield updated
  }

  override def deleteUser(email: String, password: String): Task[User] = {
    for
      user <- verifyUser(email, password)
      deleted <- repository.delete(user.id)
    yield deleted
  }

  private def verifyUser(email: String, password: String): Task[User] = {
    for
      user <- repository
        .getByEmail(email)
        .someOrFail(UnauthorizedError("Invalid email or password."))
      verified <- ZIO.attempt(
        UserServiceLive.Hasher.validateHash(password, user.hashedPassword)
      )
      verifiedUser <- ZIO
        .attempt(user)
        .when(verified)
        .someOrFail(UnauthorizedError("Invalid email or password."))
    yield verifiedUser
  }

  override def sendRecoveryToken(email: String): Task[Unit] =
    tokenRepo
      .getToken(email)
      .flatMap:
        case Some(token) => emailService.sendPasswordRecovery(email, token)
        case None => ZIO.unit

  override def recoverFromToken(email: String, token: String, newPassword: String): Task[Boolean] =
    import UserServiceLive.Hasher.generateHash
    for
      user <- repository
        .getByEmail(email)
        .someOrFail(UnauthorizedError("Invalid email or token."))
      valid <- tokenRepo.checkToken(email, token)
      result <- repository
        .update(user.id, _.copy(hashedPassword = generateHash(newPassword)))
        .when(valid)
        .map(_.nonEmpty)
    yield result
}

object UserServiceLive {
  val layer = ZLayer {
    for {
      jwtService <- ZIO.service[JWTService]
      emailService <- ZIO.service[EmailService]
      tokenRepo    <- ZIO.service[RecoveryTokensRepository]
      repository <- ZIO.service[UserRepository]
    } yield UserServiceLive(jwtService, emailService, tokenRepo, repository)
  }

  private object Hasher {
    private val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA512"
    private val PBKDF2_ITERATIONS = 1000
    private val SALT_BYTE_SIZE = 24
    private val HASH_BYTE_SIZE = 24
    private val SKF = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)

    private def pbkdf2(password: Array[Char], salt: Array[Byte], iterations: Int, nBytes: Int): Array[Byte] = {
      val spec = PBEKeySpec(password, salt, iterations, nBytes * 8)
      SKF.generateSecret(spec).getEncoded
    }

    private def toHex(bytes: Array[Byte]): String = bytes.map("%02x".format(_)).mkString
    private def fromHex(hex: String): Array[Byte] = hex.sliding(2, 2).toArray.map(Integer.parseInt(_, 16).toByte)

    def generateHash(value: String): String = {
      val rng = SecureRandom()
      val salt = Array.ofDim[Byte](SALT_BYTE_SIZE)
      rng.nextBytes(salt)

      val hashedBytes = pbkdf2(value.toCharArray, salt, PBKDF2_ITERATIONS, HASH_BYTE_SIZE)
      s"$PBKDF2_ITERATIONS:${toHex(salt)}:${toHex(hashedBytes)}"
    }

    def validateHash(password: String, hash: String): Boolean = {
      val parts = hash.split(":")
      val iterations = parts(0).toInt
      val salt = fromHex(parts(1))
      val hashed = fromHex(parts(2))

      val testHash = pbkdf2(password.toCharArray, salt, iterations, HASH_BYTE_SIZE)
      MessageDigest.isEqual(hashed, testHash)
    }
  }
}
