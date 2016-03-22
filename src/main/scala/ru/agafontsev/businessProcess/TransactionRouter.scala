package ru.agafontsev.businessProcess

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import ru.agafontsev.persistent.RedisJournalTag

import scala.concurrent.duration._

class TransactionRouter(persistentActorWatcher: ActorRef,
                        redis: RedisJournalTag) extends Actor {

  import context._

  implicit val timeout = Timeout(3 seconds)

  override def receive: Receive = {
    case env @ ConsumerEnvelope(_, _, NewTransaction(wId, tId)) =>
      redis.allPersistentIdsByTag(BusinessProcessTaggingWriteAdapter.workflowTag(wId)).onSuccess{
        case seq: Seq[String] if seq.nonEmpty =>
          seq.foreach { id =>
            (persistentActorWatcher ? GetActorByPersistenceId(id))
              .mapTo[PersistentActorRef].map(_.ref ! env)
          }

        case _ =>
          (persistentActorWatcher ? GetActorByWorkflowId(wId))
            .mapTo[PersistentActorRef]
            .foreach(_.ref ! env)
      }
  }
}
