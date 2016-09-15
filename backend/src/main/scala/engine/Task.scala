package engine

import com.typesafe.scalalogging.LazyLogging
import engine.Task.{TaskContext, TaskState, TreeNode}

object Task {

  case class TaskContext(engine: Engine, workflow: Workflow, task: Task)

  object TaskState extends Enumeration {
    val New, Done, Running = Value
  }

  trait TreeNode[T] {
    private var _parent: Option[TreeNode[T]] = None
    private var _children: List[TreeNode[T]] = List()

    def parent = _parent
    def children = _children

    def addChild(node: TreeNode[T]): Unit = {
      require(node.parent.isEmpty)
      node._parent = Some(this)
      _children = node :: _children
    }

    def value: T

    override def toString = {
      val childrenStr = if (children.isEmpty) "" else " [\n" + children.map(_.toString).mkString("\n") + "\n]"
      "(" + value.toString + childrenStr + ")"
    }
  }

}


final class Task(val id: Int, val taskDef: TaskDefinition, wf: Workflow) extends TreeNode[Task] with Cache with LazyLogging {

  private var _state: TaskState.Value = TaskState.New

  def state = _state

  def workflow: Workflow = wf

  def value: Task = this

  logger.debug("Created task \"%s\" with id %d".format(this, id))

  def execute(implicit engine: Engine): Option[ActionResult] = {
    _state = TaskState.Running
    val context = TaskContext(engine, workflow, this)
    val actionResult: Option[ActionResult] = taskDef.action(context)
    if (actionResult.isDefined) {
      logger.debug("Executed task \"%s\" with id %d".format(taskDef.name, id))
      _state = TaskState.Done
    }
    actionResult
  }

  def isExecuted: Boolean = Set(TaskState.Done) contains _state

  override def toString = taskDef.name

}

