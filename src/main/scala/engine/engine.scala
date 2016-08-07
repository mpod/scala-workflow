package engine

import com.typesafe.scalalogging.LazyLogging

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

class Cache {
  private var _intCache: Map[String, Int] = Map()
  private var _stringCache: Map[String, String] = Map()

  object ValueType extends Enumeration {
    val String, Int = Value
  }

  private def checkName(name: String, valueType: ValueType.Value): Boolean = {
    ! (valueType != ValueType.String && _stringCache.contains(name)) ||
      (valueType != ValueType.Int && _intCache.contains(name))
  }

  def setIntVal(name: String, value: Int): Unit = {
    require(checkName(name, ValueType.Int))
    _intCache += (name -> value)
  }

  def setStringVal(name: String, value: String): Unit = {
    require(checkName(name, ValueType.String))
    _stringCache += (name -> value)
  }

  def getIntVal(name: String): Int = {
    _intCache(name)
  }

  def getStringVal(name: String): String = {
    _stringCache(name)
  }
}

abstract class ActionResult
case object Ok extends ActionResult
case object Yes extends ActionResult
case object No extends ActionResult
case object JoinIsWaiting extends ActionResult

abstract class TaskDefinition {
  def action(context: TaskActionContext): Option[ActionResult]
  def name: String
}

abstract class WorkflowDefinition {
  val transitions: Map[(TaskDefinition, ActionResult), List[TaskDefinition]]
  val name: String
}

class TaskActionContext(val task: Task) {
  def workflow = task.workflow
  def engine = workflow.engine
}

object TaskState extends Enumeration {
  val New, Done, Running = Value
}

final class Task(val taskDef: TaskDefinition, val workflow: Workflow)(implicit idGen: IdGenerator)
  extends TreeNode with LazyLogging {
  private var _state: TaskState.Value = TaskState.New
  val cache = new Cache()
  val id = idGen.nextId

  def state = _state

  logger.debug("Created task \"%s\" with id %d".format(this, id))

  def execute: Option[ActionResult] = {
    _state = TaskState.Running
    val context = new TaskActionContext(this)
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

class ProcessTaskDefinition(func: (TaskActionContext) => Unit) extends TaskDefinition {
  override def action(context: TaskActionContext): Option[ActionResult] = {
    func(context)
    Some(Ok)
  }
  override def name: String = "Process"
}

class SubWorkflowTaskDefinition(wfDef: WorkflowDefinition) extends TaskDefinition {
  var wf: Option[Workflow] = None

  override def action(context: TaskActionContext): Option[ActionResult] = wf match {
    case None => wf = Some(context.engine.startWorkflow(wfDef, context.task.workflow)); None
    case Some(x) => if (x.endExecuted) Some(Ok) else None
  }

  override def name: String = "SubWorkflow[%s]".format(wfDef.name)
}

class BranchTaskDefinition(f: (TaskActionContext) => Boolean) extends TaskDefinition {
  override def action(context: TaskActionContext): Option[ActionResult] = if (f(context)) Some(Yes) else Some(No)
  override def name: String = "Branch"
}

class SplitTaskDefinition extends TaskDefinition {
  override def action(context: TaskActionContext): Option[ActionResult] = Some(Ok)
  override def name: String = "Split"
}

object StartTaskDefinition extends TaskDefinition {
  override def action(context: TaskActionContext): Option[ActionResult] = Option(Ok)
  override def name: String = "Start"
}

object EndTaskDefinition extends TaskDefinition {
  override def action(context: TaskActionContext): Option[ActionResult] = Option(Ok)
  override def name: String = "End"
}

class JoinTaskDefinition(n: Int) extends TaskDefinition {
  private var inputLines = n

  override def action(context: TaskActionContext): Option[ActionResult] = {
    inputLines -= 1
    if (inputLines == 0)
      Some(Ok)
    else
      Some(JoinIsWaiting)
  }

  override def name: String = "Join"
}

object ManualTaskDefinition {
  abstract class Field {
    type ValueType
    var _isSet = false
    def isSet = _isSet
    val label: String
    val name: String
    def setValue(value: ValueType)(implicit context: TaskActionContext)
  }

  class StringField(val label: String, val name: String) extends Field {
    type ValueType = String
    def setValue(value: ValueType)(implicit context: TaskActionContext): Unit = {
      context.task.cache.setStringVal(name, value)
      _isSet = true
    }
  }

  class IntField(val label: String, val name: String) extends Field {
    type ValueType = Int
    def setValue(value: ValueType)(implicit context: TaskActionContext): Unit = {
      context.task.cache.setIntVal(name, value)
      _isSet = true
    }
  }
}

class ManualTaskDefinition(val fields: List[ManualTaskDefinition.Field]) extends TaskDefinition {
  override def action(context: TaskActionContext): Option[ActionResult] = {
    if (fields.forall(_.isSet)) Some(Ok) else None
  }

  override def name: String = "Manual"
}

final class Workflow(wfDef: WorkflowDefinition, parent: Option[Workflow], val engine: Engine)
                    (implicit idGen: IdGenerator) extends LazyLogging {
  private var _tasks = List.empty[Task]
  val cache = new Cache()
  val id = idGen.nextId

  def tasks = _tasks
  def workflowDef = wfDef
  def parentWorkflow = parent

  def this(wfDef: WorkflowDefinition, engine: Engine)(implicit idGen: IdGenerator) = this(wfDef, None, engine)

  def start: Task = {
    val task = new Task(StartTaskDefinition, this)
    _tasks ::= task
    logger.debug("Started workflow \"%s\" with id %d".format(wfDef.name, id))
    task
  }

  def executeRound: List[Task] = for {
    t <- _tasks
    if !t.isExecuted
    r = t.execute
    if r.isDefined
    tDef <- wfDef.transitions.getOrElse((t.taskDef, r.get), List.empty[TaskDefinition])
  } yield {
    val nt = new Task(tDef, this)
    t.addChild(nt)
    _tasks ::= nt
    nt
  }

  def isStarted: Boolean = _tasks.nonEmpty

  def allExecuted: Boolean = isStarted && (_tasks forall (t => t.isExecuted))

  def endExecuted: Boolean = isStarted && (_tasks exists (t => t.taskDef == EndTaskDefinition && t.isExecuted))
}

abstract class Service

class Engine(implicit idGen: IdGenerator) {
  private var _workflows = List.empty[Workflow]

  def workflows = _workflows

  def startWorkflow(wfDef: WorkflowDefinition): Workflow = {
    startWorkflow(wfDef, None)
  }

  def startWorkflow(wfDef: WorkflowDefinition, parentWf: Workflow): Workflow = {
    startWorkflow(wfDef, Some(parentWf))
  }

  def startWorkflow(wfDef: WorkflowDefinition, parentWf: Option[Workflow]): Workflow = {
    val wf = new Workflow(wfDef, parentWf, this)
    wf.start
    _workflows ::= wf
    wf
  }

  def executeRound: Seq[Workflow] = for {
    wf <- _workflows
    if !wf.allExecuted
  } yield {
    wf.executeRound
    wf
  }
}

abstract class IdGenerator {
  def nextId: Int
}

object SimpleIdGenerator extends IdGenerator {
  var id = 0

  override def nextId: Int = {
    id += 1
    id
  }
}