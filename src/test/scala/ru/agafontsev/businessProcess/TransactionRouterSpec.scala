package ru.agafontsev.businessProcess

import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers}
import ru.agafontsev.AkkaTest
import ru.agafontsev.persistent.RedisJournalTag

import scala.concurrent.Future

class TransactionRouterSpec(_system: ActorSystem) extends AkkaTest(_system) {

  def this() = this(ActorSystem("TransactionRouterSpec"))

  trait SetupFixture {
    val persistenceActorWatcher = TestProbe()
    val persistenceActor = TestProbe()

  }

  "Transaction router" should {
    "query existing tags for new transaction" in new SetupFixture {
      val existedPersistentId = "p1"
      val redisMock = new RedisJournalTag {
        override def allPersistentIdsByTag(tag: String)(implicit actorSystem: ActorSystem): Future[Seq[String]] = {
          Future.successful(Seq(existedPersistentId))
        }
      }
      val router = system.actorOf(
        Props(
          classOf[TransactionRouter],
          persistenceActorWatcher.ref,
          redisMock)
      )

      val env = ConsumerEnvelope(self, "1", NewTransaction(WorkflowId("wId"), TransactionId("tId")))
      router ! env

      persistenceActorWatcher.expectMsg(GetActorByPersistenceId(existedPersistentId))
      persistenceActorWatcher.reply(PersistentActorRef(persistenceActor.ref))

      persistenceActor.expectMsg(env)
    }

    "ask for persistent actor creation" in new SetupFixture {
      val redisMock = new RedisJournalTag {
        override def allPersistentIdsByTag(tag: String)(implicit actorSystem: ActorSystem): Future[Seq[String]] = {
          Future.successful(Seq.empty)
        }
      }
      val router = system.actorOf(
        Props(
          classOf[TransactionRouter],
          persistenceActorWatcher.ref,
          redisMock)
      )

      val env1 = ConsumerEnvelope(self, "1", NewTransaction(WorkflowId("wId"), TransactionId("tId1")))
      val env2 = ConsumerEnvelope(self, "2", NewTransaction(WorkflowId("wId"), TransactionId("tId2")))
      router ! env1
      router ! env2

      persistenceActorWatcher.expectMsg(GetActorByWorkflowId("wId"))
      persistenceActorWatcher.reply(PersistentActorRef(persistenceActor.ref))
      persistenceActorWatcher.expectMsg(GetActorByWorkflowId("wId"))
      persistenceActorWatcher.reply(PersistentActorRef(persistenceActor.ref))

      persistenceActor.expectMsgAllOf(env1, env2)
    }
  }
}
