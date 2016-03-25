/*
 * Copyright 2016 Infotecs. All rights reserved.
 */
package ru.agafontsev.docpack

import java.util.UUID

import akka.actor.{ActorSystem, Props}
import akka.testkit._
import org.scalatest.{Matchers, WordSpecLike}
import ru.agafontsev.DilatedTimeout
import ru.agafontsev.businessProcess.{BusinessProcessId, DocPackId, DocPackStatusUpdateConfirmed, UpdateDocPackStatusByBusinessProcess}

import scala.concurrent.duration._

class DocPackPersistentSpec(_system: ActorSystem) extends TestKit(_system) with WordSpecLike with Matchers
  with ImplicitSender with DilatedTimeout {

  def this() = this(ActorSystem("DocPackPersistentSpec"))

  "DocPackPersistent" should {
    "при обновлении статуса пакета должен быть ответ" in {
      val businessProcessActorProbe = TestProbe()
      val docPackFactoryProbe = TestProbe()
      val persistenceId = UUID.randomUUID().toString

      val dpActor = system.actorOf(
        Props(
          classOf[DocPackPersistent],
          persistenceId,
          docPackFactoryProbe.ref.path)
      )
      dpActor ! UpdateDocPackStatusByBusinessProcess(
                  1,
                  BusinessProcessId("bpId"),
                  DocPackId("dpId"),
                  businessProcessActorProbe.ref)

      businessProcessActorProbe.expectMsg(10.seconds.dilated, DocPackStatusUpdateConfirmed(1))

    }
  }
}
