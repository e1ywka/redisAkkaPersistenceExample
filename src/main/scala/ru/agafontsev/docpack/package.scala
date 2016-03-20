package ru.agafontsev

package object docpack {

  /**
    * Создание пакета для существующего документооборота.
    * @param workflowId идентификатор документооборота
    */
  case class NewWorkflow(workflowId: String)

  /**
    * Запрос на преобразование существующего документооборота.
    * @param deliveryId идентификатор доставки.
    * @param workflowId идентификатор документооборота.
    */
  case class ConvertWorkflow(deliveryId: Long, workflowId: String)

  case class WorkflowConverted(deliveryId: Long, docPackId: String)

  /**
    * Оповещение об обработке создания пакета из документооборота.
    */
  case object NewWorkflowAck

  /**
    * Запрос состояния пакета.
    */
  case object GetState

  /**
    * Ответ на команду, переданную в неверное состояние.
    */
  case object WrongState
}
