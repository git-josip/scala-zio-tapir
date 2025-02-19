package com.reactive.streams

import org.apache.kafka.clients.producer.{ProducerRecord, RecordMetadata}
import zio.*
import zio.stream.*
import zio.kafka.producer.*
import zio.kafka.serde.*
import zio.json.*
import java.nio.file.Paths

object Main extends ZIOAppDefault {
  val kafkaProducerSettings: ProducerSettings =
    ProducerSettings(List("localhost:9092"))

  val producer: ZLayer[Any, Throwable, Producer] =
    ZLayer.scoped(Producer.make(kafkaProducerSettings))

  def readFile(filePath: String): ZStream[Any, Throwable, String] =
    ZStream.fromFile(Paths.get(filePath).toFile)
      .via(ZPipeline.utf8Decode)
      .via(ZPipeline.splitLines)

  def parseReview(line: String): IO[Nothing, Either[(String, String), Review]] =
    ZIO.fromEither(line.fromJson[Review])
      .tapError { error =>
        ZIO.logError(s"Error parsing line: $line, error: ${error}")
      }
      .map(Right(_)).catchAll(e => ZIO.succeed(Left((line, e))))

  def getPartitionId(asin: String, numPartitions: Int): Int = {
    (asin.hashCode & 0x7FFFFFFF) % numPartitions
  }

  def produceToKafka(review: Review, numPartitions: Int): RIO[Producer, RecordMetadata] = {
    val partitionId = getPartitionId(review.asin, numPartitions)
    val record = new ProducerRecord[String, String]("reviews-topic", partitionId, review.asin, review.toJson)
    Producer.produce(record, Serde.string, Serde.string)
  }

  def produceToDeadLetter(line: String, error: String): RIO[Producer, RecordMetadata] = {
    val dlReview = DeleterReview(
      line = line,
      error = error
    )
    val record = new ProducerRecord[String, String]("reviews-deadletter-topic", dlReview.hashCode().toString, dlReview.toJson)
    Producer.produce(record, Serde.string, Serde.string)
  }

  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] = {
    // Unzip before start file in resources/reviews/amazon_reviews.jsonl.gz
    val filePath = getClass.getResource("/reviews/amazon_reviews.jsonl").getPath

    readFile(filePath)
      .filter(line => line.startsWith("{") && line.endsWith("}"))
      .mapZIO(parseReview)
      .grouped(1000) // Adjust the chunk size as needed
      .mapChunksZIO { chunk => // Adjust the parallelism level as needed
        ZIO.foreachPar(chunk.flatten) {
          case Right(review) => produceToKafka(review, 10).retry(Schedule.exponential(1.second) && Schedule.recurs(5)).as(None)
          case Left((line, error)) => produceToDeadLetter(line, error).as(Some(line))
        }.map(Chunk.fromIterable)
      }
      .runDrain
      .provideLayer(producer)
  }
}
