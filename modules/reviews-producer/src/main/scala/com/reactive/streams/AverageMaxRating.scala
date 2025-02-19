package com.reactive.streams

import zio.*
import zio.json.*
import zio.stream.*
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

case class ProductReviewAcc(totalRating: Double, count: Int)
case class ProductReview(asin: String, averageRating: Double, reviewCount: Int)


object AverageMaxRating extends ZIOAppDefault {
  def getTopProducts: ZIO[Any, Throwable, List[ProductReview]] = {
    // Unzip before start file in resources/reviews/amazon_reviews.jsonl.gz
    val filePath = getClass.getResource("/reviews/amazon_reviews.jsonl").getPath

    for {
      ref <- Ref.make(Map.empty[String, ProductReviewAcc])
      _ <- ZStream.fromFile(Paths.get(filePath).toFile)
        .via(ZPipeline.utf8Decode)
        .via(ZPipeline.splitLines)
        .mapZIO(line => ZIO.fromEither(line.fromJson[Review]).option)
        .collectZIO { case Some(review) => ZIO.succeed(review) }
        .mapChunksZIO { chunk =>
          ZIO.foreachPar(chunk) { review =>
            ref.update { acc =>
              val productReviewAcc = acc.getOrElse(review.asin, ProductReviewAcc(0, 0))
              acc.updated(
                review.asin,
                ProductReviewAcc(
                  productReviewAcc.totalRating + review.rating,
                  productReviewAcc.count + 1
                )
              )
            }
          }
        }
        .runDrain
      acc <- ref.get
    } yield {
      acc.map { case (asin, productReview) =>
        ProductReview(asin, productReview.totalRating / productReview.count, productReview.count)
      }.toList.sortBy(-_.averageRating).take(10)
    }
  }

  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] = for {
    startTime <- Clock.currentTime(TimeUnit.MILLISECONDS)
    aggProducts <- getTopProducts
    endTime <- Clock.currentTime(TimeUnit.MILLISECONDS)
    _ <- ZIO.logInfo(s"Time taken: ${endTime - startTime} ms")
    _ <- ZIO.foreach(aggProducts)(product => ZIO.logInfo(product.toString)).unit
  } yield ()
}
