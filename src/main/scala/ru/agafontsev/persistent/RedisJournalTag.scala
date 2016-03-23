/*
 * Copyright 2016 Infotecs. All rights reserved.
 */
package ru.agafontsev.persistent

import akka.actor.ActorSystem
import redis.RedisClient
import ru.agafontsev.redis.RedisClientExtension

import scala.concurrent.Future

trait RedisJournalTag {
  def allPersistentIdsByTag(tag: String)(implicit actorSystem: ActorSystem): Future[Seq[String]]
}

object RedisJournalTagImpl extends RedisJournalTag {
  /**
    * Поиск идентификаторов акторов по указанному тэгу.
    * Тэги хранятся как ключи в redis.
    * Идентификаторы persistent actor хранятся как set.
    *
    * @param tag тэг.
    * @return идентификаторы persistent actor.
    */
  def allPersistentIdsByTag(tag: String)(implicit actorSystem: ActorSystem): Future[Seq[String]] = {
    val redis: RedisClient = RedisClientExtension(actorSystem).client
    redis.smembers[String](RedisWriteJournal.tagKey(tag))
  }

}
