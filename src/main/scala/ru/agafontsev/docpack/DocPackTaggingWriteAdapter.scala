package ru.agafontsev.docpack

import akka.persistence.journal.{Tagged, WriteEventAdapter}
import ru.agafontsev.docpack.DocPackPersistent.{ConvertWorkflowConfirmed}

object DocPackTaggingWriteAdapter {
  val WorkflowTag = "wf:"
}

/**
  * Тэгирование событий пакетов.
  */
class DocPackTaggingWriteAdapter extends WriteEventAdapter {
  import DocPackTaggingWriteAdapter._

  override def manifest(event: Any): String = ""

  override def toJournal(event: Any): Any = event match {
    // при конвертации документооборота подписываемся
    case e @ ConvertWorkflowConfirmed(_, wId, _) =>
      Tagged(e, Set(s"$WorkflowTag$wId"))
  }
}
