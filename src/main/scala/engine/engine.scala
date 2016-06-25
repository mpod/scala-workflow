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

final class Task(taskDef: TaskDefinition, workflow: Workflow) extends TreeNode {
  private var _state: TaskState.Value = TaskState.New
  def execute: Unit = {
    _state = TaskState.Running
  }
}

final class Workflow(workflowDef: WorkflowDefinition, parent: Option[Workflow]) {
  private val _tasks = mutable.ListBuffer.empty[Task]

  def this(workflowDef: WorkflowDefinition) = this(workflowDef, None)

  def start: Task = {
    val task = new Task(workflowDef.start, this)
    _tasks += task
    task
  }

  def executeRound: Unit = {
    _tasks foreach (t => t.execute)
  }
}

abstract class Service
object CacheService extends Service

object Engine {
  val _workflows = mutable.ListBuffer.empty[Workflow]
  def startWorkflow(workflowDef: WorkflowDefinition): Workflow = {
    val wf = new Workflow(workflowDef)
    _workflows += wf
    wf
  }
  def executeRound = {
    _workflows foreach (wf => wf.executeRound)
  }
}






