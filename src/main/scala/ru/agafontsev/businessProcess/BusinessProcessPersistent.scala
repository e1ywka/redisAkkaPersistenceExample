package ru.agafontsev.businessProcess

import java.util.UUID

import akka.actor._
import akka.pattern.{Backoff, BackoffSupervisor}
import akka.persistence.{AtLeastOnceDelivery, PersistentActor}

import scala.collection.mutable.{HashSet => MutableHashSet}
import scala.concurrent.duration._

class BusinessProcessPersistent(id: String,
                                businessProcessFactory: ActorPath,
                                docPackRouter: ActorRef)
  extends PersistentActor with ActorLogging with AtLeastOnceDelivery {

  import BusinessProcessPersistent._

  private var state: State = Idle
  private val processedEnvelopes = MutableHashSet.empty[String]

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

    case BusinessProcessStatusUpdated(deliveryId, bpId, dpId) =>
      persist(DocPackStatusNeedsUpdate(deliveryId, bpId, dpId))(handleEvent)

    case BusinessProcessStatusNotChanged(deliveryId) =>
      persist(RelatedWorkflowChangeConfirmed(deliveryId))(handleEvent)

    case DocPackStatusUpdateConfirmed(deliveryId) =>
      persist(DocPackStatusConfirmed(deliveryId))(handleEvent)
  }

  private def handleEvent(event: BusinessProcessEvent): Unit = event match {
    case RelatedWorkflowChanged(envId, wId, tId) =>
      deliver(businessProcessFactory)(deliveryId => UpdateBusinessProcess(deliveryId, wId, tId))
      processedEnvelopes += envId
      if (state == Idle) {
        persist(InitBusinessProcess(wId))(handleEvent)
      }

    case DocPackStatusNeedsUpdate(delvr, bpId, dpId) =>
      confirmDelivery(delvr)
      deliver(docPackRouter.path)(deliveryId => UpdateDocPackStatusByBusinessProcess(deliveryId, bpId, dpId, self))

    case DocPackStatusConfirmed(delvr) =>
      confirmDelivery(delvr)

    case RelatedWorkflowChangeConfirmed(delvr) =>
      confirmDelivery(delvr)

    case InitBusinessProcess(_) => state = Ready
  }
}

object BusinessProcessPersistent {


  sealed trait BusinessProcessEvent
  case class RelatedWorkflowChanged(envelopeCorrelationId: String,
                                    workflowId: WorkflowId,
                                    transactionId: TransactionId) extends BusinessProcessEvent
  case class DocPackStatusNeedsUpdate(deliveryId: Long, businessProcessId: BusinessProcessId, docPackId: DocPackId) extends BusinessProcessEvent
  case class DocPackStatusConfirmed(deliveryId: Long) extends BusinessProcessEvent
  case class RelatedWorkflowChangeConfirmed(deliveryId: Long) extends BusinessProcessEvent

  case class InitBusinessProcess(workflowId: WorkflowId) extends BusinessProcessEvent

  sealed trait State
  case object Idle extends State
  case object Ready extends State

  def propsWithBackoffSupervisor(persistenceId: String, businessProcessFactory: ActorPath, docPackRouter: ActorRef): Props = {
    val childProps = Props(
      new BusinessProcessPersistent(persistenceId, businessProcessFactory, docPackRouter))
    BackoffSupervisor.props(
      Backoff.onStop(
        childProps,
        s"$persistenceId-child",
        2 seconds,
        30 seconds,
        0.2)
    )
  }

  def uniquePersistentId(): String = s"business-process-persistent-${UUID.randomUUID()}"
}
