package com.reactive.ziotapir.utils

import scala.util.Random

private val asinLength = 10
private val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
def generateASIN(): String =
  (1 to asinLength).map(_ => chars(Random.nextInt(chars.length))).mkString