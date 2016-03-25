/*
 * Copyright 2016 Infotecs. All rights reserved.
 */
package ru.agafontsev.stream

import akka.actor.{ActorRef, ActorRefFactory, Status, Terminated}
import akka.stream.actor.ActorPublisher
import ru.agafontsev.businessProcess.ConsumerEnvelope

import scala.annotation.tailrec

class RabbitPublisher(consumerMaker: ActorRefFactory => ActorRef) extends ActorPublisher[ConsumerEnvelope] {
  import akka.stream.actor.ActorPublisherMessage._

  val MaxBufferSize = 100
  var buf = Vector.empty[ConsumerEnvelope]

  var consumer: Option[ActorRef] = None

  override def receive: Receive = {
    case ConsumerEnvelope(cons, _, _) if buf.size == MaxBufferSize =>
      cons ! Status.Failure(new Exception())

    case env: ConsumerEnvelope =>
      if (buf.isEmpty && totalDemand > 0) {
        onNext(env)
      } else {
        buf :+= env
        deliverBuf()
      }

    case Request(_) =>
      deliverBuf()

    case Cancel =>
      context.stop(self)

    case Terminated(ref) if consumer.contains(ref) =>
      onComplete()
  }


  @throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    consumer = Some(context.watch(consumerMaker(context)))
  }

  @tailrec
  final def deliverBuf(): Unit =
    if (totalDemand > 0) {
      /*
       * totalDemand is a Long and could be larger than
       * what buf.splitAt can accept
       */
      if (totalDemand <= Int.MaxValue) {
        val (use, keep) = buf.splitAt(totalDemand.toInt)
        buf = keep
        use foreach onNext
      } else {
        val (use, keep) = buf.splitAt(Int.MaxValue)
        buf = keep
        use foreach onNext
        deliverBuf()
      }
    }

}
