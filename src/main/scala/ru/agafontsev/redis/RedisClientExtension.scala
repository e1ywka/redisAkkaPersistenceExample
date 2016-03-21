/*
 * Copyright 2016 Infotecs. All rights reserved.
 */
package ru.agafontsev.redis

import akka.actor._
import com.typesafe.config.Config
import redis.RedisClient

class RedisClientExtensionImpl(actorSystem: ActorSystem, config: Config) extends Extension {
  private implicit val system = actorSystem
  private val host = config.getString("host")
  private val port = config.getInt("port")

  val client: RedisClient = RedisClient(
                              host = host,
                              port = port)
}

object RedisClientExtension extends ExtensionId[RedisClientExtensionImpl] with ExtensionIdProvider {
  val RedisConfig = "redis"

  override def createExtension(system: ExtendedActorSystem): RedisClientExtensionImpl =
    new RedisClientExtensionImpl(system, system.settings.config.getConfig(RedisConfig))

  override def lookup() = RedisClientExtension
}
