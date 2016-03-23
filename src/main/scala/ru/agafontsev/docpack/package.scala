package ru.agafontsev

package object docpack {

  case class UpdateDocPackStatus(deliveryId: Long, docPackId: String)

  case class DocPackStatusUpdated(deliveryId: Long)
}
