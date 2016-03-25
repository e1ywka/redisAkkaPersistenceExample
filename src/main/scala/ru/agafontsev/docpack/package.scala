package ru.agafontsev

import ru.agafontsev.businessProcess.DocPackId

package object docpack {

  case class UpdateDocPackStatus(deliveryId: Long, docPackId: DocPackId)

  case class DocPackStatusUpdated(deliveryId: Long)
}
