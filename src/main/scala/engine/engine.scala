package engine

import scala.collection.mutable

trait TreeNode {
  private var _parent: Option[TreeNode] = None
  private val _children = mutable.ListBuffer.empty[TreeNode]

  def parent = _parent
  def children = _children

  def addChild(node: TreeNode): Unit = {
    require(node.parent.isEmpty)
    node._parent = Some(this)
    children += node
  }

  def createString(value: String) = {
    val childrenStr = if (children.isEmpty) "" else " [" + children.map(_.toString).mkString(", ") + "]"
    "(" + value + childrenStr + ")"
  }
}

abstract class ActionResult
case object Done extends ActionResult
case object Yes extends ActionResult
case object No extends ActionResult

abstract class TaskDefinition {
  def action: Option[ActionResult]
  def name: String
}

abstract class WorkflowDefinition {
  val start: TaskDefinition
  val end: List[TaskDefinition]
  val transitions: Map[(TaskDefinition, ActionResult), List[TaskDefinition]]
}

object TaskState extends Enumeration {
  val New, Done, Running = Value
}

final class Task(taskDef: TaskDefinition) extends TreeNode {
  var state: TaskState.Value = TaskState.Running
  def execute = ???
}

final class Workflow(workflowDef: WorkflowDefinition) {
  val tasks = mutable.ListBuffer.empty[Task]
  def start = ???
  def execute = ???
}

abstract class Service
object CacheService extends Service

object Engine {
  val _workflows = mutable.ListBuffer.empty[Workflow]
  def startWorkflow(workflowDef: WorkflowDefinition) = ???
  def executeRound = ???
}






