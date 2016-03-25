/*
 * Copyright 2016 Infotecs. All rights reserved.
 */
package ru.agafontsev.stream

import akka.actor.ActorRef
import akka.stream.actor.ActorPublisher
import ru.agafontsev.businessProcess.{ConsumerEnvelope, NewTransaction, TransactionId, WorkflowId}

class WorkflowDbPublisher(dal: WorkflowDal) extends ActorPublisher[ConsumerEnvelope] {
  import akka.stream.actor.ActorPublisherMessage._

  import scala.concurrent.ExecutionContext.Implicits.global

  val MaxRequestItems: Int = 100

  override def receive: Receive = {

    case env: ConsumerEnvelope if isActive && totalDemand > 0 => onNext(env)

    case Request(demand) =>
      if (isActive && totalDemand > 0) {
        val currentDemand = if (totalDemand > MaxRequestItems) {
          MaxRequestItems
        } else {
          totalDemand.toInt
        }

        dal.findWithoutBusinessProcess(currentDemand)
          .foreach(
            _.map(w => ConsumerEnvelope(
                          ActorRef.noSender,
                          "",
                          NewTransaction(WorkflowId(w.workflowId.toString), TransactionId(w.initialTransaction.toString))))
            .foreach(self ! _))
      }


    case Cancel => context.stop(self)
  }
}
