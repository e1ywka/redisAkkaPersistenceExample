package ru.agafontsev.businessProcess

import java.util.UUID

import akka.actor._
import akka.persistence.{AtLeastOnceDelivery, PersistentActor}

import scala.collection.mutable.{TreeSet => MutableTreeSet}

class BusinessProcessPersistent(id: String,
                                businessProcessFactory: ActorPath,
                                docPackRouter: ActorRef)
  extends PersistentActor with ActorLogging with AtLeastOnceDelivery {

  import BusinessProcessPersistent._

  private var state: State = Idle
  private val processedEnvelopes = MutableTreeSet.empty[String]

  override def persistenceId: String = id

  override def receiveRecover: Receive = {
    case e: BusinessProcessEvent => handleEvent(e)
  }

  override def receiveCommand: Receive = {
    case ConsumerEnvelope(consumerRef, correlationId, _) if processedEnvelopes.contains(correlationId) =>
      consumerRef ! AckEnvelope(correlationId)

    case ConsumerEnvelope(consumerRef, correlationId, NewTransaction(wId, tId)) =>
      persist(RelatedWorkflowChanged(correlationId, wId, tId)) { e =>
        handleEvent(e)
        consumerRef ! AckEnvelope(correlationId)
      }

    case BusinessProcessStatusUpdated(deliveryId, bpId) =>
      persist(DocPackStatusNeedsUpdate(deliveryId, bpId))(handleEvent)

    case BusinessProcessStatusNotChanged(deliveryId) =>
      persist(RelatedWorkflowChangeConfirmed(deliveryId))(handleEvent)

    case DocPackStatusUpdated(deliveryId) =>
      persist(DocPackStatusConfirmed(deliveryId))(handleEvent)
  }

  private def handleEvent(event: BusinessProcessEvent): Unit = event match {
    case RelatedWorkflowChanged(envId, wId, tId) =>
      deliver(businessProcessFactory)(deliveryId => UpdateBusinessProcess(deliveryId, wId, tId))
      processedEnvelopes += envId
      if (state == Idle) {
        persist(InitBusinessProcess(wId))(handleEvent)
      }

    case DocPackStatusNeedsUpdate(delvr, bpId) =>
      confirmDelivery(delvr)
      deliver(docPackRouter.path)(deliveryId => UpdateDocPackStatus(deliveryId, bpId))

    case DocPackStatusConfirmed(delvr) =>
      confirmDelivery(delvr)

    case RelatedWorkflowChangeConfirmed(delvr) =>
      confirmDelivery(delvr)

    case InitBusinessProcess(_) => state = Ready
  }
}

object BusinessProcessPersistent {
  def props(/*workflowFactory: ActorSelection*/): Props = ???

  def uniquePersistentId = s"business-process:${UUID.randomUUID()}"

  sealed trait BusinessProcessEvent
  case class RelatedWorkflowChanged(envelopeCorrelationId: String,
                                    workflowId: String,
                                    transactionId: String) extends BusinessProcessEvent
  case class DocPackStatusNeedsUpdate(deliveryId: Long, businessProcessId: String) extends BusinessProcessEvent
  case class DocPackStatusConfirmed(deliveryId: Long) extends BusinessProcessEvent
  case class RelatedWorkflowChangeConfirmed(deliveryId: Long) extends BusinessProcessEvent

  case class InitBusinessProcess(workflowId: String) extends BusinessProcessEvent

  sealed trait State
  case object Idle extends State
  case object Ready extends State
}
