package ru.agafontsev

package object businessProcess {
  case class NewTransaction(workflowId: String, transactionId: String)

  case class InitBusinessProcess(workflowId: String)

  case object InitBusinessProcessComplete
}
