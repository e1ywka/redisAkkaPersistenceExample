package ru.agafontsev

import akka.actor.ActorRef

package object businessProcess {

  case class ConsumerEnvelope(consumer: ActorRef, correlationId: String, msg: AnyRef)

  case class AckEnvelope(correlationId: String)

  case class NewTransaction(workflowId: WorkflowId, transactionId: TransactionId)

  case class UpdateBusinessProcess(deliveryId: Long, workflowId: WorkflowId, transactionId: TransactionId)

  case class BusinessProcessStatusUpdated(deliveryId: Long, businessProcessId: BusinessProcessId, docPackId: DocPackId)

  case class BusinessProcessStatusNotChanged(deliveryId: Long)

  case class UpdateDocPackStatusByBusinessProcess(deliveryId: Long, businessProcessId: BusinessProcessId, docPackId: DocPackId, responseTo: ActorRef)

  case class DocPackStatusUpdateConfirmed(deliveryId: Long)

  case class WorkflowId(id: String)

  case class TransactionId(id: String)

  case class DocPackId(id: String)

  case class BusinessProcessId(id: String)
}
