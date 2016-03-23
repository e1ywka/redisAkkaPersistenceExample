package ru.agafontsev

import akka.actor.ActorRef

package object businessProcess {

  case class ConsumerEnvelope(consumer: ActorRef, correlationId: String, msg: AnyRef)

  case class AckEnvelope(correlationId: String)

  case class NewTransaction(workflowId: String, transactionId: String)

  case class UpdateBusinessProcess(deliveryId: Long, workflowId: String, transactionId: String)

  case class BusinessProcessStatusUpdated(deliveryId: Long, businessProcessId: String)

  case class BusinessProcessStatusNotChanged(deliveryId: Long)

  case class UpdateDocPackStatusByBusinessProcess(deliveryId: Long, businessProcessId: String, responseTo: ActorRef)

  case class DocPackStatusUpdateConfirmed(deliveryId: Long)
}
