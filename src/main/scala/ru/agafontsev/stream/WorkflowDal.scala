/*
 * Copyright 2016 Infotecs. All rights reserved.
 */
package ru.agafontsev.stream

import java.util.UUID

import scala.concurrent.Future

case class Workflow(workflowId: UUID, initialTransaction: UUID)

trait WorkflowDal {
  def findWithoutBusinessProcess(limit: Int): Future[Seq[Workflow]]
}
