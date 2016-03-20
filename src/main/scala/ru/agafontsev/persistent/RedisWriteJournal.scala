package ru.agafontsev.persistent

import akka.actor.ActorLogging
import akka.persistence.journal.{Tagged, AsyncWriteJournal}
import akka.persistence.{AtomicWrite, PersistentRepr}
import akka.serialization.SerializationExtension
import redis.api.Limit

import scala.collection.immutable
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

/**
  * Async write journal to Redis.
  * Writes journals in Sorted Set, using SequenceNr as score.
  *
  * Код частично позаимствован из проекта https://github.com/hootsuite/akka-persistence-redis
  */
class RedisWriteJournal extends AsyncWriteJournal with ActorLogging with RedisComponent {

  override implicit lazy val actorSystem = context.system
  implicit val ec = context.system.dispatcher
  private val serialization = SerializationExtension(context.system)
  // Redis key namespace for journals
  private def journalKey(persistenceId: String) = s"journal:$persistenceId"

  private def tagKey(tag: String) = s"journal:tags:$tag"

  private def highestSequenceNrKey(persistenceId: String) = s"${journalKey(persistenceId)}:highestSequenceNr"

  override def asyncWriteMessages(messages: immutable.Seq[AtomicWrite]): Future[immutable.Seq[Try[Unit]]] = {
    Future.sequence(messages.map(asyncWriteBatch))
  }

  private def asyncWriteBatch(atomic: AtomicWrite): Future[Try[Unit]] = {
    import Journal._
    val transaction = redis.transaction()
    transaction.watch(highestSequenceNrKey(atomic.persistenceId))

    val zAddS = atomic.payload
      .map( pr => pr.payload match {
        case Tagged(payload, tags) =>
          log.debug(s"add tags $tags")
          tags.foreach(tag => transaction.sadd(tagKey(tag), pr.persistenceId))
          pr.withPayload(payload)

        case _ => pr
      }).map( pr => {
        serialization.serialize(pr) match {
          case Success(serialized) =>
            log.debug(s"serialize message ${pr.persistenceId} - ${pr.sequenceNr}")
            val journal = Journal(pr.sequenceNr, toBase64(serialized), pr.deleted)
            transaction.zadd(journalKey(pr.persistenceId), (pr.sequenceNr, journal))
          case Failure(e) =>
            log.error(e, "serialization failed")
            Future.failed(e)
        }
      })

    val journalsF = Future.sequence(zAddS)
    val setHighestSqNrF = transaction.set(highestSequenceNrKey(atomic.persistenceId), atomic.highestSequenceNr)
    val result = for {
      _ <- journalsF
      setResult <- setHighestSqNrF
    } yield setResult

    transaction.exec()

    result.map(_ => Success(()))
      .recover({
        case e => Failure(e)
      })
  }

  def asyncDeleteMessagesTo(persistenceId: String, toSequenceNr: Long): Future[Unit] =
    redis.zremrangebyscore(journalKey(persistenceId), Limit(-1), Limit(toSequenceNr)).map{_ => ()}


  def asyncReplayMessages(persistenceId : String, fromSequenceNr : Long, toSequenceNr : Long, max : Long)
                         (replayCallback : PersistentRepr => Unit) : Future[Unit] = {
    import Journal._
    for {
      journals <- redis.zrangebyscore[Journal](
                    journalKey(persistenceId),
                    Limit(fromSequenceNr),
                    Limit(toSequenceNr),
                    Some((0L, max)))
    } yield {
      journals.foreach { journal =>
        serialization.deserialize(fromBase64(journal.persistenceReprBase64),  classOf[PersistentRepr]) match {
          case Success(repr) => replayCallback(repr)
          case Failure(e) => Future.failed(new RuntimeException(e))
        }
      }
    }
  }

  def asyncReadHighestSequenceNr(persistenceId : String, fromSequenceNr : Long) : Future[Long] = {
    redis.get(highestSequenceNrKey(persistenceId)).map {
      highestSequenceNr => highestSequenceNr.map(_.utf8String.toLong).getOrElse(0L)
    }
  }
}
