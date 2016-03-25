/*
 * Copyright 2016 Infotecs. All rights reserved.
 */
package ru.agafontsev.docpack

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import ru.agafontsev.businessProcess._
import ru.agafontsev.persistent.RedisJournalTag

import scala.concurrent.duration._

class DocPackRouter(persistentActorWatcher: ActorRef,
                    redis: RedisJournalTag) extends Actor {
  import context._
  implicit val timeout = Timeout(3 seconds)

  def receive: Receive = {
    case msg @ UpdateDocPackStatusByBusinessProcess(_, _, DocPackId(dpId), _) =>
      redis.allPersistentIdsByTag(DocPackTaggingWriteAdapter.docPackTag(dpId)).onSuccess{
        case seq: Seq[String] if seq.nonEmpty =>
          seq.foreach { id =>
            (persistentActorWatcher ? GetDocPackByPersistenceId(id))
              .mapTo[PersistentActorRef].map(_.ref ! msg)
          }

        case _ =>
          (persistentActorWatcher ? GetDocPackByDocPackId(dpId))
            .mapTo[PersistentActorRef]
            .foreach(_.ref ! msg)
      }
  }
}
