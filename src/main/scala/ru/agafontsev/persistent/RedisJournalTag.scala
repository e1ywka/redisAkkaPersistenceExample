/*
 * Copyright 2016 Infotecs. All rights reserved.
 */
package ru.agafontsev.persistent

import redis.RedisClient

object RedisJournalTag {
  /**
    * Поиск идентификаторов акторов по указанному тэгу.
    * Тэги хранятся как ключи в redis.
    * Идентификаторы persistent actor хранятся как set.
    *
    * @param tag тэг.
    * @return идентификаторы persistent actor.
    */
  def allPersistentIdsByTag(tag: String): Set[String] = ???

}
