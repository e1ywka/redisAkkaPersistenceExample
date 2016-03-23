/*
 * Copyright 2016 Infotecs. All rights reserved.
 */
package ru.agafontsev.businessProcess

import java.util.UUID

import akka.actor.{ActorSystem, Props}
import akka.testkit._
import com.typesafe.config.ConfigFactory
import org.scalatest.{Matchers, WordSpecLike}
import ru.agafontsev.DilatedTimeout

import scala.concurrent.duration._

class BusinessProcessPersistentSpec(_system: ActorSystem) extends TestKit(_system) with WordSpecLike with Matchers
  with ImplicitSender with DilatedTimeout {

  def this() = this(ActorSystem("BusinessProcessPersistentSpec", ConfigFactory.load()))

  "Business process" should {
    "при повторном запросе с тем correlationId запрос не обрабатывается" in {
      val docPackRouterProbe = TestProbe()
      val businessProcessFactory = TestProbe()
      val persistentId = UUID.randomUUID().toString
      val workflowId = UUID.randomUUID().toString

      val bpActor = system.actorOf(Props(
        classOf[BusinessProcessPersistent],
        persistentId,
        businessProcessFactory.ref.path,
        docPackRouterProbe.ref)
      )
      within(10.seconds.dilated) {
        bpActor ! ConsumerEnvelope(self, "corId", NewTransaction(workflowId, "tId"))
        expectMsg(AckEnvelope("corId"))
        businessProcessFactory.expectMsg(UpdateBusinessProcess(1, workflowId, "tId"))
        bpActor ! ConsumerEnvelope(self, "corId", NewTransaction(workflowId, "tId"))
        expectMsg(AckEnvelope("corId"))
        businessProcessFactory.expectNoMsg()
      }
    }

    "при обновлении статуса BusinessProcess может потребоваться обновление статуса пакета" in {
      val docPackRouterProbe = TestProbe()
      val businessProcessFactory = TestProbe()
      val persistentId = UUID.randomUUID().toString
      val workflowId = UUID.randomUUID().toString

      val bpActor = system.actorOf(Props(
        classOf[BusinessProcessPersistent],
        persistentId,
        businessProcessFactory.ref.path,
        docPackRouterProbe.ref)
      )
      within(10.seconds.dilated) {
        bpActor ! ConsumerEnvelope(self, "corId", NewTransaction(workflowId, "tId"))
        expectMsg(AckEnvelope("corId"))
        businessProcessFactory.expectMsg(UpdateBusinessProcess(1, workflowId, "tId"))
        businessProcessFactory.reply(BusinessProcessStatusUpdated(1, "bpId"))
        docPackRouterProbe.expectMsg(UpdateDocPackStatus(2, "bpId"))
        docPackRouterProbe.reply(DocPackStatusUpdated(2))
      }
    }

    "если обновление статуса business process не произошло, то и пакет обновлять не нужно" in {
      val docPackRouterProbe = TestProbe()
      val businessProcessFactory = TestProbe()
      val persistentId = UUID.randomUUID().toString
      val workflowId = UUID.randomUUID().toString

      val bpActor = system.actorOf(Props(
        classOf[BusinessProcessPersistent],
        persistentId,
        businessProcessFactory.ref.path,
        docPackRouterProbe.ref)
      )
      within(10.seconds.dilated) {
        bpActor ! ConsumerEnvelope(self, "corId", NewTransaction(workflowId, "tId"))
        expectMsg(AckEnvelope("corId"))
        businessProcessFactory.expectMsg(UpdateBusinessProcess(1, workflowId, "tId"))
        businessProcessFactory.reply(BusinessProcessStatusNotChanged(1))
        docPackRouterProbe.expectNoMsg()
      }
    }
  }
}
