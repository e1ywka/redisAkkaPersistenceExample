package ru.agafontsev.businessProcess

import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import org.scalatest.{FlatSpecLike, Matchers}
import ru.agafontsev.persistent.RedisJournalTag

import scala.concurrent.Future

class TransactionRouterSpec(_system: ActorSystem) extends TestKit(_system)
  with FlatSpecLike with Matchers with ImplicitSender {
  def this() = this(ActorSystem("TransactionRouterSpec"))

  "Transaction router" should "query existing tags for new transaction" in {
    val existedPersistentId = "p1"
    val persistenceActorWatcher = TestProbe()
    val persistenceActor = TestProbe()

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

    val env = ConsumerEnvelope(self, "1", NewTransaction("wId", "tId"))
    router ! env

    persistenceActorWatcher.expectMsg(GetActorByPersistenceId(existedPersistentId))
    persistenceActorWatcher.reply(PersistentActor(persistenceActor.ref))

    persistenceActor.expectMsg(env)
  }

  "Transaction router" should "ask for persistent actor creation" in {
    val persistenceActorWatcher = TestProbe()
    val persistenceActor = TestProbe()

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

    val env1 = ConsumerEnvelope(self, "1", NewTransaction("wId", "tId"))
    val env2 = ConsumerEnvelope(self, "2", NewTransaction("wId", "tId2"))
    router ! env1
    router ! env2

    persistenceActorWatcher.expectMsg(GetActorByWorkflowId("wId"))
    persistenceActorWatcher.reply(PersistentActor(persistenceActor.ref))
    persistenceActorWatcher.expectMsg(GetActorByWorkflowId("wId"))
    persistenceActorWatcher.reply(PersistentActor(persistenceActor.ref))

    persistenceActor.expectMsgAllOf(env1, env2)
  }
}
