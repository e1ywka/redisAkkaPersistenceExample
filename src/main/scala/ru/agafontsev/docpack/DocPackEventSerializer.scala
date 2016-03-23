package ru.agafontsev.docpack

import akka.serialization.SerializerWithStringManifest
import ru.agafontsev.docpack.DocPackPersistent._

class DocPackEventSerializer extends SerializerWithStringManifest {
  import java.nio.charset.StandardCharsets.UTF_8

  override def identifier: Int = 1000

  override def manifest(o: AnyRef): String = o match {
    case obj: UpdateDocPack => "UpdateDocPack.1"
    case obj: DocPackUpdated => "DocPackUpdated.1"
  }

  override def fromBinary(bytes: Array[Byte], manifest: String): AnyRef = manifest match {
    case "UpdateDocPack.1" =>
      val split = new String(bytes, UTF_8).split("|")
      UpdateDocPack(split(0))

    case "DocPackUpdated.1" =>
      val split = new String(bytes, UTF_8).split("|")
      DocPackUpdated(split(0).toLong)

    case m => throw new IllegalArgumentException(
      s"Unable to deserialize from bytes, manifest was: $manifest! Bytes length: ${bytes.length}")
  }


  override def toBinary(o: AnyRef): Array[Byte] = o match {
    case UpdateDocPack(bpId) => s"$bpId".getBytes(UTF_8)

    case DocPackUpdated(d) => s"$d".getBytes(UTF_8)

    case _ => throw new IllegalArgumentException(
      s"Unable to serialize to bytes, clazz was: ${o.getClass}!")
  }
}
