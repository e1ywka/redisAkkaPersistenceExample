/*
 * Copyright 2016 Infotecs. All rights reserved.
 */
package ru.agafontsev.docpack

import akka.persistence.journal.{Tagged, WriteEventAdapter}
import ru.agafontsev.businessProcess.DocPackId
import ru.agafontsev.docpack.DocPackPersistent.InitDocPackPersistent

class DocPackTaggingWriteAdapter extends WriteEventAdapter {
  import DocPackTaggingWriteAdapter._

  override def manifest(event: Any): String = ""

  override def toJournal(event: Any): Any = event match {
    case InitDocPackPersistent(DocPackId(dpId)) => Tagged(event, Set(docPackTag(dpId)))
  }
}

object DocPackTaggingWriteAdapter {
  def docPackTag(docPackId: String): String = s"docpack:$docPackId"
}
