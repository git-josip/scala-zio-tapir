package com.reactive.ziotapir.syntax

import zio.*
import zio.test.*

extension [R,E,A](zio: ZIO[R,E,A])
  def assert(assertion: Assertion[A]): ZIO[R,E,TestResult] = assertZIO(zio)(assertion)

  def assert(assertionName: => String)(predicate: (=> A) => Boolean): ZIO[R,E,TestResult] = assert(Assertion.assertion(assertionName)(predicate))
  def assert(predicate: (=> A) => Boolean): ZIO[R,E,TestResult] = assert("test assertion")(predicate)