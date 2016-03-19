package ru.agafontsev.persistent

import akka.actor.ActorSystem
import redis.RedisClient

trait RedisComponent {
  implicit val actorSystem: ActorSystem

  private val config = actorSystem.settings.config
  private val host = config.getString("redis.host")
  private val port = config.getInt("redis.port")

  lazy val redis = new RedisClient(host, port)
}
