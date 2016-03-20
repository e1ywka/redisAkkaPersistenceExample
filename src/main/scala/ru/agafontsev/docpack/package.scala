package ru.agafontsev

package object docpack {

  /**
    * Новая транзакция.
    * @param workflowId legacy-идентификатор документооборота.
    * @param transactionId идентификатор транзакции.
    */
  case class NewTransaction(workflowId: String, transactionId: String)

  /**
    * Запрос на преобразование существующего документооборота.
    * @param deliveryId идентификатор доставки.
    * @param workflowId идентификатор документооборота.
    */
  case class ConvertWorkflow(deliveryId: Long, workflowId: String)

  case class WorkflowConverted(deliveryId: Long, workflowId: String, docPackId: String)

  case class NewDocPack(docPackId: String)

  /**
    * Оповещение об обработке создания пакета из документооборота.
    */
  case object NewWorkflowAck
  
  case class AddDocument(documentId: String)

  case class UpdateDocPackStatus(deliveryId: Long, docPackId: String, transactionId: String)

  case class DocPackStatusUpdated(deliveryId: Long)

  /**
    * Запрос состояния пакета.
    */
  case object GetState

  /**
    * Ответ на команду, переданную в неверное состояние.
    */
  case object WrongState
}
