package ru.agafontsev.businessProcess

class BusinessProcessTaggingWriteAdapter {

}

object BusinessProcessTaggingWriteAdapter {
  def workflowTag(workflowId: String) = s"wf:$workflowId"
}
