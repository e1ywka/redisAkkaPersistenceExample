/*
 * Copyright 2016 Infotecs. All rights reserved.
 */
package ru.agafontsev.businessProcess

import akka.serialization.SerializerWithStringManifest
import ru.agafontsev.businessProcess.BusinessProcessPersistent._

class BusinessProcessEventSerializer extends SerializerWithStringManifest {
  import java.nio.charset.StandardCharsets.UTF_8

  import BusinessProcessEventSerializer._

  override def identifier: Int = 1001

  override def manifest(o: AnyRef): String = o match {
    case _: RelatedWorkflowChanged => RelatedWorkflowChanged_v1
    case _: InitBusinessProcess => InitBusinessProcess_v1
    case _: DocPackStatusNeedsUpdate => DocPackStatusNeedsUpdate_v1
    case _: DocPackStatusConfirmed => DocPackStatusConfirmed_v1
    case _: RelatedWorkflowChangeConfirmed => RelatedWorkflowChangeConfirmed_v1
  }

  override def fromBinary(bytes: Array[Byte], manifest: String): AnyRef = manifest match {
    case RelatedWorkflowChanged_v1 =>
      val split = new String(bytes, UTF_8).split("|")
      RelatedWorkflowChanged(split(0), split(1), split(2))

    case InitBusinessProcess_v1 =>
      val split = new String(bytes, UTF_8).split("|")
      InitBusinessProcess(split(0))

    case DocPackStatusNeedsUpdate_v1 =>
      val split = new String(bytes, UTF_8).split("|")
      DocPackStatusNeedsUpdate(split(0).toLong, split(1))

    case DocPackStatusConfirmed_v1 => DocPackStatusConfirmed(new String(bytes, UTF_8).toLong)

    case RelatedWorkflowChangeConfirmed_v1 => RelatedWorkflowChangeConfirmed(new String(bytes, UTF_8).toLong)
  }

  override def toBinary(o: AnyRef): Array[Byte] = o match {
    case RelatedWorkflowChanged(envId, wId, tId) => s"$envId|$wId|$tId".getBytes(UTF_8)

    case InitBusinessProcess(wId) => s"$wId".getBytes(UTF_8)

    case DocPackStatusNeedsUpdate(deliveryId, bpId) => s"$deliveryId|$bpId".getBytes(UTF_8)

    case DocPackStatusConfirmed(deliveryId) => s"$deliveryId".getBytes(UTF_8)

    case RelatedWorkflowChangeConfirmed(deliveryId) => s"$deliveryId".getBytes(UTF_8)
  }
}

object BusinessProcessEventSerializer {

  val RelatedWorkflowChanged_v1 = "RelatedWorkflowChanged.1"
  val InitBusinessProcess_v1 = "InitBusinessProcess.1"
  val DocPackStatusNeedsUpdate_v1 = "DocPackStatusNeedsUpdate.1"
  val DocPackStatusConfirmed_v1 = "DocPackStatusConfirmed.1"
  val RelatedWorkflowChangeConfirmed_v1 = "RelatedWorkflowChangeConfirmed.1"
}