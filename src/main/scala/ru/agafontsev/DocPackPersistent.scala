package ru.agafontsev

import java.util.UUID

import akka.actor.ActorPath
import akka.persistence.{AtLeastOnceDelivery, PersistentActor}

object DocPackPersistent {
  case class NewWorkflow(workflowId: String)
  case class ConvertWorkflow(deliveryId: Long, workflowId: String)
  case class WorkflowConverted(deliveryId: Long, docPackId: String)
  case object NewWorkflowAck
  case object GetState
  case object WrongState

  sealed trait DocPackEvent
  case class ConvertWorkflowSent(workflowId: String) extends DocPackEvent
  case class ConvertWorkflowConfirmed(deliveryId: Long, docPackId: String) extends DocPackEvent

  sealed trait State
  case object Idle extends State
  case class NewWorkflowProcessing(workflowId: String) extends State
  case class DocPackCreated(docPackId: String) extends State
}

class DocPackPersistent(docPackId: String, docPackFactory: ActorPath) extends PersistentActor with AtLeastOnceDelivery {
  import DocPackPersistent._

  override def persistenceId: String = s"docpack:$docPackId"

  private var state: State = Idle

  override def receiveRecover: Receive = {
    case e: DocPackEvent => handleEvent(e)
  }

  override def receiveCommand: Receive = {
    case NewWorkflow(wId) if state == Idle => persist(ConvertWorkflowSent(wId))(handleEvent)
    case NewWorkflow(_) => sender() ! WrongState
    case WorkflowConverted(deliveryId, dpId) => persist(ConvertWorkflowConfirmed(deliveryId, dpId))(handleEvent)
    case GetState => sender() ! state
  }

  def handleEvent(e: DocPackEvent) = e match {
    case ConvertWorkflowSent(wId) =>
      deliver(docPackFactory)(deliveryId => ConvertWorkflow(deliveryId, wId))
      state = NewWorkflowProcessing(wId)
      sender() ! NewWorkflowAck
    case ConvertWorkflowConfirmed(deliveryId, dpId) =>
      confirmDelivery(deliveryId)
      state = DocPackCreated(dpId)
  }
}
