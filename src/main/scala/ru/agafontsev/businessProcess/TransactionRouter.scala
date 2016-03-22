package ru.agafontsev.businessProcess

import akka.actor._
import akka.actor.Actor.Receive
import akka.pattern.ask
import akka.util.Timeout
import ru.agafontsev.persistent.{RedisJournalTag}

import scala.collection.mutable
import scala.concurrent.duration._

class TransactionRouter extends Actor {

  import context._

  private val pendingBpCreations: mutable.HashMap[String, ActorRef] = mutable.HashMap.empty
  private val existedCreations: mutable.HashMap[ActorRef, String] = mutable.HashMap.empty

  implicit val timeout = Timeout(3 seconds)

  override def receive: Receive = {
    case t@NewTransaction(wId, tId) =>
      val persistentIds: Set[String] = RedisJournalTag.allPersistentIdsByTag(BusinessProcessTaggingWriteAdapter.workflowTag(wId))
      if (persistentIds.nonEmpty) {
        persistentIds.foreach { persistenceId =>
          system.actorSelection(s"/user/$persistenceId").ask(Identify(1)).map {
            case ActorIdentity(_, Some(ref)) => ref forward t

            case ActorIdentity(_, None) =>
              system.actorOf(BusinessProcessPersistent.props(), persistenceId) forward t
          }
        }
      } else {
        val creator = pendingBpCreations.getOrElseUpdate(wId, {
          val persistentActor = context.system.actorOf(
            BusinessProcessPersistent.props(),
            BusinessProcessPersistent.uniquePersistentId)
          val c = context.actorOf(Props(classOf[BusinessProcessCreator], self, persistentActor))
          existedCreations += c -> wId
          context.watch(c)
        })
        creator forward t
      }

    case Terminated(creator) =>
      existedCreations.get(creator) match {
        case Some(wId) =>
          existedCreations.remove(creator)
          pendingBpCreations.remove(wId)

        case None => existedCreations.remove(creator)
      }
  }
}

class BusinessProcessCreator( router: ActorRef,
                              persistentActor: ActorRef) extends Actor with Stash {

  def receive: Receive = idle

  def idle: Receive = {
    case t@NewTransaction(workflowId, _) =>
      persistentActor ! InitBusinessProcess(workflowId)
      persistentActor forward t
      context.become(creatingPersistentActor(workflowId))
  }

  def creatingPersistentActor(workflowId: String): Receive = {
    case t: NewTransaction => persistentActor forward t

    case InitBusinessProcessComplete =>
      unstashAll()
      self ! PoisonPill
  }
}

