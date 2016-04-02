/*
 * Copyright 2016 Infotecs. All rights reserved.
 */
package ru.agafontsev.stream

import akka.actor.{ActorRef, ActorRefFactory, ActorSystem, Props}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Keep, Source}
import akka.stream.testkit.scaladsl.TestSink
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import org.scalamock.scalatest.MockFactory
import org.scalatest.{FlatSpecLike, Matchers}
import ru.agafontsev.AkkaTest
import ru.agafontsev.businessProcess.{ConsumerEnvelope, NewTransaction, TransactionId, WorkflowId}

class RabbitPublisherSpec(_system: ActorSystem) extends AkkaTest(_system) with MockFactory {

  def this() = this(ActorSystem("RabbitPublisherSpec"))

  implicit val materializer = ActorMaterializer()

  "RabbitPublisher" should {
    "при наличие запроса передавать новое событие в поток" in {
      val consumerProbe = TestProbe()

      val makerMock = mockFunction[ActorRefFactory, ActorRef]
      makerMock expects(*) returning consumerProbe.ref once()

      val (pub, sub) = Source.actorPublisher(Props(new RabbitPublisher(makerMock)))
        .toMat(TestSink.probe[ConsumerEnvelope])(Keep.both)
        .run()

      watch(pub)

      pub ! ConsumerEnvelope(self, "1", NewTransaction(WorkflowId("1"), TransactionId("1")))
      sub.request(1).expectNext(
        ConsumerEnvelope(self, "1", NewTransaction(WorkflowId("1"), TransactionId("1")))
      )
    }
  }
}
