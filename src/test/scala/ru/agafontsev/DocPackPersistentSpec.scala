package ru.agafontsev

import akka.actor.{Props, ActorSystem}
import akka.testkit.{ImplicitSender, TestProbe, TestKit}
import com.typesafe.config.{ConfigFactory, Config}
import org.scalatest.{BeforeAndAfterAll, Matchers, FlatSpecLike}
import DocPackPersistent._

import scala.concurrent.Await
import scala.concurrent.duration._

class DocPackPersistentSpec(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
  with FlatSpecLike with Matchers with BeforeAndAfterAll {

  def this() = this(ActorSystem("DocPackPersistentSpec", ConfigFactory.parseString(
    """
      |akka.loggers = ["akka.event.slf4j.Slf4jLogger"]
      |akka.loglevel = DEBUG
      |akka.persistence.journal.plugin = "akka.persistence.journal.inmem"
      |akka.persistence.snapshot-store.plugin = "akka.persistence.snapshot-store.local"
    """.stripMargin)))

  override def afterAll() = {
    Await.ready(system.terminate(), 3 seconds)
  }

  "DocPack" should "be created on new workflow" in {
    val docPackFactory = TestProbe()
    val docPackPersistence = system.actorOf(Props(new DocPackPersistent(docPackFactory.ref.path)))
    docPackPersistence ! NewWorkflow("w1")
    expectMsg(NewWorkflowAck)

    var deliveryId: Long = 0
    docPackFactory.expectMsgPF() {
      case ConvertWorkflow(dId, "w1") => deliveryId = dId
    }
    docPackFactory.reply(WorkflowConverted(deliveryId, "dp1"))

    docPackPersistence ! GetState
    expectMsg(DocPackCreated("dp1"))
  }

  "NewWorkflow" should "return WrongState if actor is not in the Idle state" in {
    val docPackFactory = TestProbe()
    val docPackPersistence = system.actorOf(Props(new DocPackPersistent(docPackFactory.ref.path)))
    docPackPersistence ! NewWorkflow("w1")
    expectMsg(NewWorkflowAck)
    docPackPersistence ! NewWorkflow("w2")
    expectMsg(WrongState)
  }
}
