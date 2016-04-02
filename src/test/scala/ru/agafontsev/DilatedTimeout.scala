/*
 * Copyright 2016 Infotecs. All rights reserved.
 */
package ru.agafontsev

import akka.testkit.TestKit
import com.typesafe.config.ConfigFactory
import org.scalatest.concurrent.ScaledTimeSpans

import scala.util.{Failure, Success, Try}

/**
  * Изменение временных интервалов scalatest с учетом параметра akka.test.timefactor.
  */
trait DilatedTimeout extends ScaledTimeSpans { this: TestKit =>
  override def spanScaleFactor: Double =
    Try(system.settings.config.getDouble("akka.test.timefactor")) match {
      case Success(timefactor) => timefactor
      case Failure(_) => 1.0
    }
}