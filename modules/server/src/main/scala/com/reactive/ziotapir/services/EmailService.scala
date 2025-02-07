package com.reactive.ziotapir.services

import com.reactive.ziotapir.config.{Configs, EmailServiceConfig}
import zio.*

import java.util.Properties
import javax.mail.internet.MimeMessage
import javax.mail.{
  Authenticator,
  Message,
  PasswordAuthentication,
  Session,
  Transport
}

trait EmailService:
  def sendEmail(to: String, subject: String, content: String): Task[Unit]
  def sendPasswordRecovery(to: String, token: String): Task[Unit] =
    val subject = "Reactive ZioTapir: Password Recovery"
    val content =
      s"""
         |<div style="
         |  border: 1px solid black;
         |  padding: 20px;
         |  font-family: sans-serif;
         |  line-height: 2;
         |  font-size: 20px;
         |">
         |  <h1>Reactive ZioTapir: Password Recovery</h1>
         |  <p>Your password recovery token is: <strong>$token</strong></p>
         |  <p>:) from Reactive</p>
         |</div>
         |""".stripMargin
    sendEmail(to, subject, content)
end EmailService

class EmailServiceLive private (config: EmailServiceConfig)
  extends EmailService:
  override def sendEmail(to: String, subject: String, content: String): Task[Unit] =
    for
      props   <- propsResource
      session <- createSession(props)
      message <- createMessage(session)("reactive@ziotapir.com", to, subject, content)
    yield Transport.send(message)

  private val propsResource: Task[Properties] =
    val props = Properties()
    props.put("mail.smtp.auth", true)
    props.put("mail.smtp.starttls.enable", "true")
    props.put("mail.smtp.host", config.host)
    props.put("mail.smtp.port", config.port)
    props.put("mail.smtp.ssl.trust", config.host)
    ZIO.succeed(props)

  private def createSession(props: Properties): Task[Session] =
    ZIO.attempt:
      Session.getInstance(
        props,
        new Authenticator:
          override def getPasswordAuthentication: PasswordAuthentication =
            PasswordAuthentication(config.user, config.pass)
      )

  private def createMessage(session: Session)(from: String, to: String, subject: String, content: String): Task[MimeMessage] =
    val message = MimeMessage(session)
    message.setFrom(from)
    message.setRecipients(Message.RecipientType.TO, to)
    message.setSubject(subject)
    message.setContent(content, "text/html; charset=utf-8")
    ZIO.succeed(message)
end EmailServiceLive

object EmailServiceLive:
  val layer: ZLayer[EmailServiceConfig, Nothing, EmailService] = ZLayer {
    ZIO
      .service[EmailServiceConfig]
      .map(EmailServiceLive(_))
  }

  val configuredLayer: ZLayer[Any, Throwable, EmailService] =
    Configs.makeLayer[EmailServiceConfig]("ziotapir.email") >>> layer
end EmailServiceLive

