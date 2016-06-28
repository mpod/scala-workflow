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
case object Ok extends ActionResult
case object Yes extends ActionResult
case object No extends ActionResult

abstract class TaskDefinition {
  def action: Option[ActionResult]
  def name: String
}

abstract class WorkflowDefinition {
  val taskDefinitions: List[TaskDefinition]
  val start: TaskDefinition
  val end: List[TaskDefinition]
  val transitions: Map[(TaskDefinition, ActionResult), List[TaskDefinition]]
}

object TaskState extends Enumeration {
  val New, Done, Running = Value
}

final class Task(val taskDef: TaskDefinition, workflow: Workflow) extends TreeNode {
  private var _state: TaskState.Value = TaskState.New

  def execute: Option[ActionResult] = {
    _state = TaskState.Running
    val r = taskDef.action
    if (r.isDefined) {
      _state = TaskState.Done
    }
    r
  }

  def isExecuted: Boolean = Set(TaskState.Done) contains _state

  override def toString = taskDef.name
}

final class Workflow(workflowDef: WorkflowDefinition, parent: Option[Workflow]) {
  private val _tasks = mutable.ListBuffer.empty[Task]

  def this(workflowDef: WorkflowDefinition) = this(workflowDef, None)

  def start: Task = {
    val task = new Task(workflowDef.start, this)
    _tasks += task
    println("Started workflow with task: " + task)
    task
  }

  def executeRound: List[Task] = {
    val newTasksHelper: mutable.ListBuffer[List[Task]] = for {
      t <- _tasks
      if !t.isExecuted
      r <- t.execute
      tDefs <- workflowDef.transitions.get((t.taskDef, r))
      newTasks = for {
        tDef <- tDefs
        nt = new Task(tDef, this)
        _ = t.addChild(nt)
      } yield nt
    } yield newTasks
    val newTasks = newTasksHelper.toList.flatten
    newTasks foreach (x => _tasks += x)
    println("Created new tasks: " + newTasks)
    newTasks
  }

  def isExecuted: Boolean = _tasks forall (t => t.isExecuted)
}

abstract class Service

object CacheService extends Service
object EngineService extends Service

object Engine {
  val _workflows = mutable.ListBuffer.empty[Workflow]

  def startWorkflow(workflowDef: WorkflowDefinition): Workflow = {
    val wf = new Workflow(workflowDef)
    wf.start
    _workflows += wf
    wf
  }

  def executeRound: Seq[Workflow] = for {
    wf <- _workflows
    if !wf.isExecuted
    dummy = wf.executeRound
  } yield wf
}






