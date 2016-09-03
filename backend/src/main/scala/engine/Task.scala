package engine

import com.typesafe.scalalogging.LazyLogging
import engine.Task.{TaskActionContext, TaskState, TreeNode}

object Task {

  class TaskActionContext(val task: Task) {
    def workflow = task.workflow
    def engine = workflow.engine
  }

  object TaskState extends Enumeration {
    val New, Done, Running = Value
  }

  trait TreeNode {
    private var _parent: Option[TreeNode] = None
    private var _children: List[TreeNode] = List()

    def parent = _parent
    def children = _children

    def addChild(node: TreeNode): Unit = {
      require(node.parent.isEmpty)
      node._parent = Some(this)
      _children = node :: _children
    }

    def valueToString: String

    override def toString = {
      val childrenStr = if (children.isEmpty) "" else " [\n" + children.map(_.toString).mkString("\n") + "\n]"
      "(" + valueToString + childrenStr + ")"
    }
  }

}


final class Task(val taskDef: TaskDefinition, val workflow: Workflow)(implicit idGen: IdGenerator)
  extends TreeNode with LazyLogging {
  private var _state: TaskState.Value = TaskState.New
  val cache = new Cache()
  val id = idGen.nextId
  private val _context = new TaskActionContext(this)

  implicit def context = _context

  def state = _state

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
  override def valueToString: String = taskDef.name
}

