/*
 * Copyright 2016 Infotecs. All rights reserved.
 */
package ru.agafontsev.businessProcess

import java.util.UUID

import akka.actor._
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import org.scalamock.scalatest.MockFactory
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers}

class PersistentActorWatcherSpec(_system: ActorSystem) extends TestKit(_system)
with FlatSpecLike with Matchers with ImplicitSender with BeforeAndAfterAll with MockFactory {

  def this() = this(ActorSystem("PersistentActorWatcherSpec"))

  override protected def afterAll() = {
    system.terminate()
  }

  "Watcher" should "create new actor for workflow id and cache it" in {
    val persistentActorProbe = TestProbe()
    val persistenceId = "1"
    val makerMock = mockFunction[ActorRefFactory, String, ActorRef]
    makerMock expects(*, persistenceId) returning persistentActorProbe.ref once()

    def uniqueId(): String = UUID.randomUUID().toString

    val watcher = system.actorOf(Props(new PersistentActorWatcher(makerMock, () => persistenceId)))

    watcher ! GetActorByWorkflowId("wId")
    expectMsg(PersistentActorRef(persistentActorProbe.ref))

    watcher ! GetActorByWorkflowId("wId")
    expectMsg(PersistentActorRef(persistentActorProbe.ref))
  }

  "Watcher" should "create new actor for workflow id and return same actor for persistence id" in {
    val persistentActorProbe = TestProbe()
    val persistenceId = "1"
    val makerMock = mockFunction[ActorRefFactory, String, ActorRef]
    makerMock expects(*, persistenceId) returning persistentActorProbe.ref once()

    def uniqueId(): String = UUID.randomUUID().toString

    val watcher = system.actorOf(Props(new PersistentActorWatcher(makerMock, () => persistenceId)))

    watcher ! GetActorByWorkflowId("wId")
    expectMsg(PersistentActorRef(persistentActorProbe.ref))

    watcher ! GetActorByPersistenceId(persistenceId)
    expectMsg(PersistentActorRef(persistentActorProbe.ref))
  }
}
