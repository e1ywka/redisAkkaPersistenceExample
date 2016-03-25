package ru.agafontsev.docpack

import java.util.UUID

import akka.actor._
import akka.pattern.{Backoff, BackoffSupervisor}
import akka.persistence.{AtLeastOnceDelivery, PersistentActor}
import ru.agafontsev.businessProcess.{BusinessProcessId, DocPackId, DocPackStatusUpdateConfirmed, UpdateDocPackStatusByBusinessProcess}

import scala.concurrent.duration._

class DocPackPersistent(id: String, docPackFactory: ActorPath) extends PersistentActor with AtLeastOnceDelivery {
  import DocPackPersistent._

  private var state: State = Idle

  override def persistenceId: String = id

  override def receiveRecover: Receive = {
    case e: DocPackPersistentEvent => handleEvent(e)
  }

  override def receiveCommand: Receive = {
    case UpdateDocPackStatusByBusinessProcess(deliveryId, businessProcessId, docPackId, responseTo) =>
      persist(UpdateDocPack(businessProcessId, docPackId)) { e =>
        handleEvent(e)
        responseTo ! DocPackStatusUpdateConfirmed(deliveryId)
      }

    case DocPackStatusUpdated(deliveryId) => persist(DocPackUpdated(deliveryId))(handleEvent)
  }

  def handleEvent(e: DocPackPersistentEvent): Unit = e match {
    case UpdateDocPack(businessProcessId, docPackId) =>
      deliver(docPackFactory)(deliveryId => UpdateDocPackStatus(deliveryId, docPackId))
      if (state == Idle) {
        persist(InitDocPackPersistent(docPackId))(handleEvent)
      }

    case DocPackUpdated(delvr) => confirmDelivery(delvr)

    case InitDocPackPersistent(_) => state = Ready
  }
}

object DocPackPersistent {

  sealed trait DocPackPersistentEvent

  case class UpdateDocPack(businessProcessId: BusinessProcessId, docPackId: DocPackId) extends DocPackPersistentEvent

  case class DocPackUpdated(deliveryId: Long) extends DocPackPersistentEvent

  case class InitDocPackPersistent(docPackId: DocPackId) extends DocPackPersistentEvent

  sealed trait State

  case object Idle extends State

  case object Ready extends State

  def uniquePersistentId(): String = s"docpack-persistent-${UUID.randomUUID()}"

  def propsWithBackoffSupervisor(persistenceId: String, docPackFactory: ActorPath): Props = {
    val childProps = Props(new DocPackPersistent(persistenceId, docPackFactory))
    BackoffSupervisor.props(
      Backoff.onStop(
        childProps,
        s"$persistenceId-child",
        2 seconds,
        30 seconds,
        0.2)
    )
  }
}