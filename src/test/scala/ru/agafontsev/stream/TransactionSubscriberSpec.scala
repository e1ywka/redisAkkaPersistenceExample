/*
 * Copyright 2016 Infotecs. All rights reserved.
 */
package ru.agafontsev.stream

import akka.actor.{ActorSystem, Props}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Keep, Sink}
import akka.stream.testkit.scaladsl.TestSource
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import org.scalamock.scalatest.MockFactory
import org.scalatest.{FlatSpecLike, Matchers}
import ru.agafontsev.AkkaTest
import ru.agafontsev.businessProcess._

class TransactionSubscriberSpec(_system: ActorSystem) extends AkkaTest(_system) {

  def this() = this(ActorSystem("TransactionSubscriberSpec"))

  implicit val materializer = ActorMaterializer()

  "TransactionSubscriber" should {
    "заменяет ссылку на себя" in {
      val transactionRouterProbe = TestProbe()
      val consumerProbe = TestProbe()

      val (pub, sub) = TestSource.probe[ConsumerEnvelope]
        .toMat(Sink.actorSubscriber(Props(new TransactionSubscriber(transactionRouterProbe.ref))))(Keep.both)
        .run()

      pub.sendNext(ConsumerEnvelope(consumerProbe.ref, "1", NewTransaction(WorkflowId("1"), TransactionId("1"))))

      transactionRouterProbe.expectMsg(ConsumerEnvelope(sub, "1", NewTransaction(WorkflowId("1"), TransactionId("1"))))
      transactionRouterProbe.reply(AckEnvelope("1"))
      consumerProbe.expectMsg(akka.camel.Ack)
    }
  }
}
