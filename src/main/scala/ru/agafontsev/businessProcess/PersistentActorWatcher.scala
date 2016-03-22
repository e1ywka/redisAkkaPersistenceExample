/*
 * Copyright 2016 Infotecs. All rights reserved.
 */
package ru.agafontsev.businessProcess

import akka.actor.{Terminated, Actor, ActorRef, Props}
import akka.dispatch.sysmsg.Terminate

import scala.collection.mutable

case class GetActorByPersistenceId(persistenceId: String)
case class GetActorByWorkflowId(workflowId: String)
case class PersistentActor(ref: ActorRef)

class PersistentActorWatcher(childProps: String => Props, uniquePersistenceId: () => String) extends Actor {

  val persistentActorByWorkflowId: mutable.HashMap[String, ActorRef] = mutable.HashMap.empty
  val workflowIdByActor: mutable.HashMap[ActorRef, String] = mutable.HashMap.empty

  val persistentActorByPersistentId: mutable.HashMap[String, ActorRef] = mutable.HashMap.empty
  val persistentIdByActor: mutable.HashMap[ActorRef, String] = mutable.HashMap.empty

  def receive: Receive = {
    case GetActorByPersistenceId(persistenceId) =>
      val child = persistentActorByPersistentId.getOrElseUpdate(persistenceId, {
        context.watch(context.actorOf(childProps(persistenceId)))
      })
      sender() ! PersistentActor(child)

    case GetActorByWorkflowId(workflowId) =>
      val ref = persistentActorByWorkflowId.getOrElseUpdate(workflowId, {
        val id = uniquePersistenceId()
        val child = context.watch(context.actorOf(childProps(id)))
        persistentActorByPersistentId += id -> child
        child
      })
      sender() ! PersistentActor(ref)

    case Terminated(ref) =>
      workflowIdByActor.get(ref) match {
        case Some(wId) =>
          workflowIdByActor.remove(ref)
          persistentActorByWorkflowId.remove(wId)

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
