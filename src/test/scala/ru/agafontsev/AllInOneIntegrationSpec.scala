/*
 * Copyright 2016 Infotecs. All rights reserved.
 */
package ru.agafontsev

import java.util.UUID

import akka.actor.{ActorRef, ActorRefFactory, ActorSystem, Props}
import akka.testkit.TestActor.{AutoPilot, KeepRunning}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import org.scalatest.{Matchers, WordSpecLike}
import ru.agafontsev.businessProcess._
import ru.agafontsev.docpack.{DocPackPersistent, DocPackRouter, DocPackWatcher}
import ru.agafontsev.persistent.RedisJournalTagImpl

import scala.concurrent.duration._

class AllInOneIntegrationSpec(_system: ActorSystem) extends AkkaTest(_system) {

  def this() = this(ActorSystem("AllInOneIntegrationSpec"))

  trait AllSetupFixture {
    val consumerProbe = TestProbe()

    val businessProcessFactoryProbe = TestProbe()
    businessProcessFactoryProbe.setAutoPilot(new AutoPilot {
      override def run(sender: ActorRef, msg: Any): AutoPilot = msg match {
        case UpdateBusinessProcess(delvr, _, _) =>
          sender ! BusinessProcessStatusUpdated(delvr, BusinessProcessId("1"), DocPackId("1"))
          KeepRunning
      }
    })
    val docPackFactoryProbe = TestProbe()

    def dpMaker(context: ActorRefFactory, persistentId: String): ActorRef = {
      context.actorOf(
        DocPackPersistent.propsWithBackoffSupervisor(persistentId, docPackFactoryProbe.ref.path))
    }

    val dpWatcher = system.actorOf(Props(new DocPackWatcher(dpMaker, DocPackPersistent.uniquePersistentId)))

    val docPackRouter = system.actorOf(Props(new DocPackRouter(dpWatcher, RedisJournalTagImpl)))

    def bpPersistentActorMaker(context: ActorRefFactory, persistentId: String): ActorRef = {
      context.actorOf(
        BusinessProcessPersistent.propsWithBackoffSupervisor(
          persistentId,
          businessProcessFactoryProbe.ref.path,
          docPackRouter)
      )
    }

    val bpWatcher = system.actorOf(
      Props(new PersistentActorWatcher(bpPersistentActorMaker, BusinessProcessPersistent.uniquePersistentId)))

    val transactionRouter = system.actorOf(Props(new TransactionRouter(bpWatcher, RedisJournalTagImpl)))
  }

  "Интеграционный тест" should {
    "каждое обновление business process вызывает обновление пакета" in new AllSetupFixture {
      val workflowId = WorkflowId(UUID.randomUUID().toString)

      consumerEnvelopesForWorkflow(consumerProbe.ref, workflowId).take(10).foreach { env =>
        transactionRouter ! env
      }

      businessProcessFactoryProbe.receiveN(10, 10.seconds)
      docPackFactoryProbe.receiveN(10, 10.seconds)
    }

    "разные business process вызывает обновление одного и того же пакета" in new AllSetupFixture {
      consumerEnvelopes(consumerProbe.ref).take(10).foreach { env =>
        transactionRouter ! env
      }

      businessProcessFactoryProbe.receiveN(10, 10.seconds)
      docPackFactoryProbe.receiveN(10, 10.seconds)
    }
  }

  def consumerEnvelopesForWorkflow(consumer: ActorRef, workflowId: WorkflowId): Stream[ConsumerEnvelope] = {
    def newEnvelope(corrId: Int): Stream[ConsumerEnvelope] = {
      ConsumerEnvelope(
        consumer,
        corrId.toString,
        NewTransaction(workflowId, TransactionId(UUID.randomUUID().toString))) #:: newEnvelope(corrId + 1)
    }

    newEnvelope(1)
  }

  def consumerEnvelopes(consumer: ActorRef): Stream[ConsumerEnvelope] = {
    def newEnvelope(corrId: Int): Stream[ConsumerEnvelope] = {
      ConsumerEnvelope(
        consumer,
        corrId.toString,
        NewTransaction(WorkflowId(UUID.randomUUID().toString), TransactionId(UUID.randomUUID().toString))) #:: newEnvelope(corrId + 1)
    }

    newEnvelope(1)
  }
}
