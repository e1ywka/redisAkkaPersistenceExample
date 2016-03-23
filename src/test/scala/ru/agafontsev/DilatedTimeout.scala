/*
 * Copyright 2016 Infotecs. All rights reserved.
 */
package ru.agafontsev

import com.typesafe.config.ConfigFactory
import org.scalatest.concurrent.ScaledTimeSpans

/**
  * Изменение временных интервалов scalatest с учетом параметра akka.test.timefactor.
  */
trait DilatedTimeout extends ScaledTimeSpans {
  override def spanScaleFactor: Double =
    ConfigFactory.load().getDouble("akka.test.timefactor")
}