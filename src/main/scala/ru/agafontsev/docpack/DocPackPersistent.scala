package ru.agafontsev.docpack

import akka.actor._
import akka.pattern.{Backoff, BackoffSupervisor}
import akka.pattern.ask
import akka.persistence.{AtLeastOnceDelivery, PersistentActor}
import akka.util.Timeout

import scala.collection.immutable.{Seq => Seq}
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

object DocPackPersistent {

  /**
    * Событие, сохраняемое в журнал.
    */
  sealed trait DocPackEvent

  /**
    * Событие отправки запроса на конвертацию документооборота.
    * @param workflowId
    */
  case class ConvertWorkflowSent(workflowId: String, transactionId: String) extends DocPackEvent

  /**
    * Событие успешного выполнения конвертации документооборота.
    * @param deliveryId
    * @param docPackId
    */
  case class ConvertWorkflowConfirmed(deliveryId: Long, docPackId: String) extends DocPackEvent

  case class UpdateDocPackStatusSent(transactionId: String) extends DocPackEvent

  case class DocPackStatusUpdateConfirmed(deliveryId: Long) extends DocPackEvent

  /**
    * Внутреннее состояние актора.
    */
  sealed trait State

  /**
    * Начальное состояние.
    */
  case object Idle extends State

  /**
    * Обработка команды о новом документе.
    * @param workflowId
    */
  case class NewWorkflowProcessing(workflowId: String, transactionId: String) extends State

  /**
    * Создан пакет.
    * @param docPackId
    */
  case class DocPackCreated(docPackId: String) extends State

  def findDocPack(docPackId: String)
                 (implicit system: ActorSystem, timeout: Timeout, ec: ExecutionContext): Future[ActorRef] = {

    system.actorSelection(system / s"user/docpack-$docPackId") ? Identify(docPackId) map {
      case ActorIdentity(`docPackId`, Some(actorRef)) => actorRef

      case ActorIdentity(`docPackId`, None) =>
        //todo вынести название docPackFactory в константы
        system.actorOf(
          docPackPropsWithBackoff(docPackId, ActorPath.fromString("/user/docPackFactory")),
          s"docpack-$docPackId")
    }
  }

  def docPackPropsWithBackoff(docPackId: String, docPackFactory: ActorPath): Props = {
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

class DocPackPersistent(persistentId: String, docPackFactory: ActorPath) extends PersistentActor with AtLeastOnceDelivery {
  import DocPackPersistent._

  override def persistenceId: String = s"docpack:$persistentId"

  private var state: State = Idle

  override def receiveRecover: Receive = {
    case e: DocPackEvent => handleEvent(e)
  }

  override def receiveCommand: Receive = {
    // Конвертация документооборота из legacy-формата.
    case NewTransaction(wId, trId) if state == Idle =>
      persist(ConvertWorkflowSent(wId, trId))(handleEvent)


    case NewTransaction(_, _) => sender() ! WrongState

    // Добавление документа в новый пакет
    case AddDocument(docId) if state == Idle =>

    case WorkflowConverted(deliveryId, dpId) => persist(ConvertWorkflowConfirmed(deliveryId, dpId))(handleEvent)

    case DocPackStatusUpdated(deliveryId) => persist(DocPackStatusUpdateConfirmed(deliveryId))(handleEvent)

    case GetState => sender() ! state
  }

  def handleEvent(e: DocPackEvent): Unit = e match {
    case ConvertWorkflowSent(wId, trId) =>
      deliver(docPackFactory)(deliveryId => ConvertWorkflow(deliveryId, wId))
      state = NewWorkflowProcessing(wId, trId)
      sender() ! NewWorkflowAck

    case ConvertWorkflowConfirmed(deliveryId, dpId) =>
      confirmDelivery(deliveryId)
      state match {
        case NewWorkflowProcessing(wId, trId) =>
          state = DocPackCreated(dpId)
          persist(UpdateDocPackStatusSent(trId))(handleEvent)
      }

    case UpdateDocPackStatusSent(trId) =>
      state match {
        case DocPackCreated(docPackId) =>
          deliver(docPackFactory)(deliveryId => UpdateDocPackStatus(deliveryId, docPackId, trId))
      }

    case DocPackStatusUpdateConfirmed(deliveryId) =>
      confirmDelivery(deliveryId)
  }
}
