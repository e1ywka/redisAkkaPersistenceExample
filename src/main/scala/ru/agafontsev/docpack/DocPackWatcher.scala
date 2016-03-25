/*
 * Copyright 2016 Infotecs. All rights reserved.
 */
package ru.agafontsev.docpack

import akka.actor.{Actor, ActorRef, ActorRefFactory, Terminated}
import ru.agafontsev.businessProcess.PersistentActorRef

import scala.collection.mutable

case class GetDocPackByPersistenceId(persistenceId: String)

case class GetDocPackByDocPackId(docPackId: String)

case class DocPackPersistentActorRef(ref: ActorRef)

class DocPackWatcher(childMaker: (ActorRefFactory, String) => ActorRef, uniquePersistenceId: () => String) extends Actor {
  val persistentActorByDocPackId: mutable.HashMap[String, ActorRef] = mutable.HashMap.empty
  val docPackIdByActor: mutable.HashMap[ActorRef, String] = mutable.HashMap.empty

  val persistentActorByPersistentId: mutable.HashMap[String, ActorRef] = mutable.HashMap.empty
  val persistentIdByActor: mutable.HashMap[ActorRef, String] = mutable.HashMap.empty

  def receive: Receive = {
    case GetDocPackByPersistenceId(persistenceId) =>
      val child = persistentActorByPersistentId.getOrElseUpdate(persistenceId, {
        context.watch(childMaker(context, persistenceId))
      })
      sender() ! PersistentActorRef(child)

    case GetDocPackByDocPackId(workflowId) =>
      val ref = persistentActorByDocPackId.getOrElseUpdate(workflowId, {
        val id = uniquePersistenceId()
        val child = context.watch(childMaker(context, id))
        persistentActorByPersistentId += id -> child
        child
      })
      sender() ! PersistentActorRef(ref)

    case Terminated(ref) =>
      docPackIdByActor.get(ref) match {
        case Some(wId) =>
          docPackIdByActor.remove(ref)
          persistentActorByDocPackId.remove(wId)

        case None =>
      }
      persistentIdByActor.get(ref) match {
        case Some(persistentId) =>
          persistentActorByPersistentId.remove(persistentId)
          persistentIdByActor.remove(ref)

        case None =>
      }
  }
}
