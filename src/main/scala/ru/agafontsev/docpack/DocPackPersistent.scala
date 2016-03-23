package ru.agafontsev.docpack

import akka.actor._
import akka.pattern.{Backoff, BackoffSupervisor}
import akka.persistence.{AtLeastOnceDelivery, PersistentActor}
import ru.agafontsev.businessProcess.{DocPackStatusUpdateConfirmed, UpdateDocPackStatusByBusinessProcess}

import scala.collection.immutable.Seq
import scala.concurrent.duration._

class DocPackPersistent(id: String, docPackFactory: ActorPath) extends PersistentActor with AtLeastOnceDelivery {
  import DocPackPersistent._

  override def persistenceId: String = id

  override def receiveRecover: Receive = {
    case e: DocPackPersistentEvent => handleEvent(e)
  }

  override def receiveCommand: Receive = {
    case UpdateDocPackStatusByBusinessProcess(deliveryId, businessProcessId, responseTo) =>
      persist(UpdateDocPack(businessProcessId)) { e =>
        handleEvent(e)
        responseTo ! DocPackStatusUpdateConfirmed(deliveryId)
      }

    case DocPackStatusUpdated(deliveryId) => persist(DocPackUpdated(deliveryId))(handleEvent)
  }

  def handleEvent(e: DocPackPersistentEvent): Unit = e match {
    case UpdateDocPack(businessProcessId) =>
      deliver(docPackFactory)(deliveryId => UpdateDocPackStatus(deliveryId, businessProcessId))

    case DocPackUpdated(delvr) => confirmDelivery(delvr)
  }
}

object DocPackPersistent {

  sealed trait DocPackPersistentEvent

  case class UpdateDocPack(businessProcessId: String) extends DocPackPersistentEvent

  case class DocPackUpdated(deliveryId: Long) extends DocPackPersistentEvent

  def propsWithBackoffSupervisor(docPackId: String, docPackFactory: ActorPath): Props = {
    val childProps = Props(new DocPackPersistent(docPackId, docPackFactory))
    BackoffSupervisor.props(
      Backoff.onStop(
        childProps,
        s"docpack-$docPackId-persist",
        2 seconds,
        30 seconds,
        0.2)
    )
  }
}