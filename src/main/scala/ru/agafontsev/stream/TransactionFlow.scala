/*
 * Copyright 2016 Infotecs. All rights reserved.
 */
package ru.agafontsev.stream

import akka.actor.{ActorRef, ActorRefFactory, Props}
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{Keep, Merge, Sink, Source}

object TransactionFlow {

  def apply(dal: WorkflowDal, rabbitConsumer: ActorRefFactory => ActorRef, transactionRouter: ActorRef) = {
    val dbSource = Source.actorPublisher(Props(new WorkflowDbPublisher(dal)))
      .buffer(100, OverflowStrategy.backpressure)
    val queueSource = Source.actorPublisher(Props(new RabbitPublisher(rabbitConsumer)))

    Source.combine(dbSource, queueSource)(Merge(_))
        .toMat(Sink.actorSubscriber(Props(new TransactionSubscriber(transactionRouter))))(Keep.right)
  }
}
