/*
 * Copyright 2016 Infotecs. All rights reserved.
 */
package ru.agafontsev.businessProcess

import java.util.UUID

import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import com.typesafe.config.ConfigFactory
import org.scalatest.{Matchers, WordSpecLike}
import ru.agafontsev.persistent.RedisJournalTagImpl

import scala.concurrent.Await
import scala.concurrent.duration._

class BusinessProcessPersistentSpec(_system: ActorSystem) extends TestKit(_system) with WordSpecLike with Matchers
  with ImplicitSender {

  def this() = this(ActorSystem("BusinessProcessPersistentSpec", ConfigFactory.load()))

  "Business process" should {
    "create workflow tag on first transaction" in {
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
      within(10 seconds) {
        bpActor ! ConsumerEnvelope(self, "corId", NewTransaction(workflowId, "tId"))
        expectMsg(AckEnvelope("corId"))
      }

      val persistentIds = Await.result(
        RedisJournalTagImpl.allPersistentIdsByTag(BusinessProcessTaggingWriteAdapter.workflowTag(workflowId)), 3 seconds)
      persistentIds should contain(persistentId)
    }
  }
}
