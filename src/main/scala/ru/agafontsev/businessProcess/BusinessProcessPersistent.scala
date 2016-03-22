package ru.agafontsev.businessProcess

import java.util.UUID

import akka.actor.{ActorSelection, Props, ActorRef, ActorSystem}
import akka.persistence.PersistentActor

class BusinessProcessPersistent(id: String) extends PersistentActor {
  override def persistenceId: String = id

  override def receiveRecover: Receive = ???

  override def receiveCommand: Receive = {
    case t @ NewTransaction(wId, trId) => persist() { e =>
      sender() ! InitBusinessProcessComplete
    }
  }


}

object BusinessProcessPersistent {
  def props(/*workflowFactory: ActorSelection*/): Props = ???

  def uniquePersistentId = s"business-process:${UUID.randomUUID()}"
}
