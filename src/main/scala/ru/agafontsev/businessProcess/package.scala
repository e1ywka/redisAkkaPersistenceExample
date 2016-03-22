package ru.agafontsev

import akka.actor.ActorRef

package object businessProcess {

  case class ConsumerEnvelope(consumer: ActorRef, correlationId: String, msg: AnyRef)

  case class NewTransaction(workflowId: String, transactionId: String)

  case class InitBusinessProcess(workflowId: String)

  case object InitBusinessProcessComplete
}
