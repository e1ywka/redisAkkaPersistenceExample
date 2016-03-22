package ru.agafontsev.businessProcess

import akka.actor.{Terminated, Props, ActorSystem}
import akka.testkit.{ImplicitSender, TestProbe, TestKit}
import org.scalatest.{Matchers, FlatSpecLike}

class BusinessProcessCreatorSpec(_system: ActorSystem) extends TestKit(_system)
with FlatSpecLike with Matchers with ImplicitSender {
  def this() = this(ActorSystem("BusinessProcessCreatorSpec"))

  "Creator" should "init persistent actor and wait for reply" in {
    val persistentActorProbe = TestProbe()
    val creator = system.actorOf(Props(classOf[BusinessProcessCreator], self, persistentActorProbe.ref))
    watch(creator)

    creator ! NewTransaction("wId", "tId")
    creator ! NewTransaction("wId", "tId1")
    creator ! NewTransaction("wId", "tId2")
    persistentActorProbe.expectMsg(InitBusinessProcess("wId"))
    persistentActorProbe.reply(InitBusinessProcessComplete)
    persistentActorProbe.expectMsg(NewTransaction("wId", "tId"))
    persistentActorProbe.expectMsg(NewTransaction("wId", "tId1"))
    persistentActorProbe.expectMsg(NewTransaction("wId", "tId2"))

    expectTerminated(creator)
  }
}
