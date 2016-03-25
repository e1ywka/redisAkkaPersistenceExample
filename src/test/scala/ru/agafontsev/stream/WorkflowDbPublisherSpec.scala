/*
 * Copyright 2016 Infotecs. All rights reserved.
 */
package ru.agafontsev.stream

import java.util.UUID

import akka.actor.{ActorSystem, Props}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Keep, Source}
import akka.stream.testkit.scaladsl.TestSink
import akka.testkit.{ImplicitSender, TestKit}
import org.scalamock.scalatest.MockFactory
import org.scalatest.{FlatSpecLike, Matchers}
import ru.agafontsev.businessProcess.ConsumerEnvelope

import scala.concurrent.Future

class WorkflowDbPublisherSpec(_system: ActorSystem) extends TestKit(_system)
  with FlatSpecLike with MockFactory with Matchers with ImplicitSender {

  def this() = this(ActorSystem("WorkflowDbPublisherSpec"))

  implicit val materializer = ActorMaterializer()

  "WorkflowDbPublisher" should "request demanded elements" in {
    var demanded: Int = 0

    val workflowDalMock = mock[WorkflowDal]
    (workflowDalMock.findWithoutBusinessProcess _) expects(*) onCall { arg: Int =>
      demanded = demanded + arg
      Future.successful(generateWorkflows().take(arg))
    }
    val (pub, sub) = Source.actorPublisher[ConsumerEnvelope](Props(new WorkflowDbPublisher(workflowDalMock)))
      .toMat(TestSink.probe[ConsumerEnvelope])(Keep.both)
      .run()
    watch(pub)

    sub.request(100)
    sub.expectNextN(100)
    sub.cancel()
    expectTerminated(pub)
    demanded should be(100)
  }

  def generateWorkflows(): Stream[Workflow] = {
    def loop(): Stream[Workflow] = {
      Workflow(UUID.randomUUID(), UUID.randomUUID()) #:: loop()
    }
    loop()
  }
}
