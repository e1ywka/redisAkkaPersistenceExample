/*
 * Copyright 2016 Infotecs. All rights reserved.
 */
package ru.agafontsev.businessProcess

import java.util.UUID

import akka.actor._
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import org.scalamock.scalatest.MockFactory
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers}
import ru.agafontsev.AkkaTest

class PersistentActorWatcherSpec(_system: ActorSystem) extends AkkaTest(_system) with MockFactory {

  def this() = this(ActorSystem("PersistentActorWatcherSpec"))

  trait SetupFixture {
    val persistentActorProbe = TestProbe()
    val persistenceId = "1"
    val makerMock = mockFunction[ActorRefFactory, String, ActorRef]
    makerMock expects(*, persistenceId) returning persistentActorProbe.ref once()

    def uniqueId(): String = UUID.randomUUID().toString

    val watcher = system.actorOf(Props(new PersistentActorWatcher(makerMock, () => persistenceId)))

  }

  "Watcher" should {
    "create new actor for workflow id and cache it" in new SetupFixture {
      watcher ! GetActorByWorkflowId("wId")
      expectMsg(PersistentActorRef(persistentActorProbe.ref))

      watcher ! GetActorByWorkflowId("wId")
      expectMsg(PersistentActorRef(persistentActorProbe.ref))
    }

    "create new actor for workflow id and return same actor for persistence id" in new SetupFixture {
      watcher ! GetActorByWorkflowId("wId")
      expectMsg(PersistentActorRef(persistentActorProbe.ref))

      watcher ! GetActorByPersistenceId(persistenceId)
      expectMsg(PersistentActorRef(persistentActorProbe.ref))
    }
  }
}
