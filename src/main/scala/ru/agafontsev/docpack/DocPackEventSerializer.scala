package ru.agafontsev.docpack

import akka.serialization.SerializerWithStringManifest
import ru.agafontsev.docpack.DocPackPersistent.{ConvertWorkflowConfirmed, ConvertWorkflowSent}

class DocPackEventSerializer extends SerializerWithStringManifest {
  import java.nio.charset.StandardCharsets.UTF_8

  override def identifier: Int = 1000

  override def manifest(o: AnyRef): String = o match {
    case obj: ConvertWorkflowSent => "ConvertWorkflowSent.1"
    case obj: ConvertWorkflowConfirmed => "ConvertWorkflowConfirmed.1"
  }

  override def fromBinary(bytes: Array[Byte], manifest: String): AnyRef = manifest match {
    case "ConvertWorkflowSent.1" => ConvertWorkflowSent(new String(bytes, UTF_8))

    case "ConvertWorkflowConfirmed.1" =>
      val split = new String(bytes, UTF_8).split("|")
      ConvertWorkflowConfirmed(split(0).toLong, split(1))

    case m => throw new IllegalArgumentException(
      s"Unable to deserialize from bytes, manifest was: $manifest! Bytes length: ${bytes.length}")
  }


  override def toBinary(o: AnyRef): Array[Byte] = o match {
    case ConvertWorkflowSent(wId) => wId.getBytes(UTF_8)

    case ConvertWorkflowConfirmed(d, dpId) => s"$d|$dpId".getBytes(UTF_8)

    case _ => throw new IllegalArgumentException(
      s"Unable to serialize to bytes, clazz was: ${o.getClass}!")
  }
}
