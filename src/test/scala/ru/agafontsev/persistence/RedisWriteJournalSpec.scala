package ru.agafontsev.persistence

import akka.persistence.CapabilityFlag
import akka.persistence.journal.JournalSpec
import com.typesafe.config.ConfigFactory

class RedisWriteJournalSpec extends JournalSpec(ConfigFactory.load()) {

  override def supportsRejectingNonSerializableObjects: CapabilityFlag =
    false

}
