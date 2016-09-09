package engine

import com.typesafe.scalalogging.LazyLogging
import engine.Task.{TaskContext, TaskState, TreeNode}

object Task {

  class TaskContext(val task: Task) {
    def workflow = task.workflow
    def engine = workflow.engine
  }

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


final class Task(val taskDef: TaskDefinition, val workflow: Workflow)(implicit idGen: IdGenerator)
  extends TreeNode[Task] with Cache with LazyLogging {

  private var _state: TaskState.Value = TaskState.New
  val id = idGen.nextId
  private val _context = new TaskContext(this)

  implicit def context = _context

  def state = _state

  def value = this

  logger.debug("Created task \"%s\" with id %d".format(this, id))

  def execute: Option[ActionResult] = {
    _state = TaskState.Running
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

