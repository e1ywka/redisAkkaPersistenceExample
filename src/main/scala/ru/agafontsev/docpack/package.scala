package ru.agafontsev

package object docpack {
  case class NewWorkflow(workflowId: String)
  case class ConvertWorkflow(deliveryId: Long, workflowId: String)
  case class WorkflowConverted(deliveryId: Long, docPackId: String)
  case object NewWorkflowAck
  case object GetState
  case object WrongState
}
