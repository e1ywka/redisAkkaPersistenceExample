package ru.agafontsev

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

abstract class AkkaTest(_system: ActorSystem) extends TestKit(_system) with WordSpecLike with Matchers
with ImplicitSender with DilatedTimeout with BeforeAndAfterAll {

  override protected def afterAll() = {
    system.terminate()
  }
}
