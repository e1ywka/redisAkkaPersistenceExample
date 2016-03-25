/*
 * Copyright 2016 Infotecs. All rights reserved.
 */
package ru.agafontsev.stream

import akka.actor.ActorRef
import akka.stream.actor.{ActorSubscriber, ActorSubscriberMessage, MaxInFlightRequestStrategy, RequestStrategy}
import ru.agafontsev.businessProcess.{AckEnvelope, ConsumerEnvelope}

class TransactionSubscriber(transactionRouter: ActorRef) extends ActorSubscriber {
  import ActorSubscriberMessage._
  val MaxQueueSize = 10
  var queue = Map.empty[String, ActorRef]

  protected def requestStrategy: RequestStrategy = new MaxInFlightRequestStrategy(MaxQueueSize) {
    override def inFlightInternally: Int = queue.size
  }

  def receive: Receive = {
    case OnNext(env @ ConsumerEnvelope(consumer, corrId, _)) =>
      queue += corrId -> consumer
      transactionRouter ! env.copy(consumer = self)

    case AckEnvelope(corrId) =>
      queue.get(corrId) match {
        case Some(ref) =>
          ref ! akka.camel.Ack
          queue -= corrId

        case None => // nop
      }
  }
}
