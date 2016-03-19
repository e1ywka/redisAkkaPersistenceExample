package ru.agafontsev.persistent

import java.util.Base64

import akka.util.ByteString
import redis.ByteStringFormatter

/**
  * Journal entry that can be serialized and deserialized to JSON
  * JSON in turn is serialized to ByteString so it can be stored in Redis with Rediscala
  */
case class Journal(sequenceNr: Long, persistenceReprBase64: String, deleted: Boolean)

object Journal {

  import org.json4s._
  import org.json4s.jackson.Serialization
  import org.json4s.jackson.Serialization.{read, write}

  implicit val formats = Serialization.formats(NoTypeHints)

  implicit val byteStringFormatter = new ByteStringFormatter[Journal] {
    override def serialize(data: Journal): ByteString = {
      ByteString(write(data))
    }

    override def deserialize(bs: ByteString): Journal = {
      try {
        read[Journal](bs.utf8String)
      } catch {
        case e: Exception => throw new RuntimeException(e)
      }
    }
  }

  def toBase64(bytes: Array[Byte]): String = {
    Base64.getEncoder.encodeToString(bytes)
  }

  def fromBase64(base64Encoded: String): Array[Byte] = {
    Base64.getDecoder.decode(base64Encoded)
  }
}
