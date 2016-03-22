package ru.agafontsev.businessProcess

import akka.actor.ActorSystem
import akka.testkit.TestKit
import org.scalatest.{Matchers, FlatSpecLike}

class TransactionRouterSpec(_system: ActorSystem) extends TestKit(_system)
  with FlatSpecLike with Matchers {
  def this() = this(ActorSystem("TransactionRouterSpec"))


}
