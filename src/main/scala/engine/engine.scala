package engine

import scala.collection.mutable

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

abstract class ActionResult
case object Ok extends ActionResult
case object Yes extends ActionResult
case object No extends ActionResult
case object JoinIsWaiting extends ActionResult

abstract class TaskDefinition {
  def action: Option[ActionResult]
  def name: String
}

abstract class WorkflowDefinition {
  val transitions: Map[(TaskDefinition, ActionResult), List[TaskDefinition]]
  val name: String
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
      println("Executed task \"%s\"".format(taskDef.name))
      _state = TaskState.Done
    }
    r
  }

  def isExecuted: Boolean = Set(TaskState.Done) contains _state
  override def toString = taskDef.name
  override def valueToString: String = taskDef.name
}

class ProcessTaskDefinition(f: () => Unit) extends TaskDefinition {
  override def action: Option[ActionResult] = {
    f()
    Some(Ok)
  }
  override def name: String = "Process"
}

class SubWorkflowTaskDefinition(wfDef: WorkflowDefinition) extends TaskDefinition {
  var wf: Option[Workflow] = None

  override def action: Option[ActionResult] = wf match {
    case None => wf = Some(Engine.startWorkflow(wfDef)); None
    case Some(x) => if (x.endExecuted) Some(Ok) else None
  }

  override def name: String = "SubWorkflow[%s]".format(wfDef.name)
}

class BranchTaskDefinition(f: () => Boolean) extends TaskDefinition {
  override def action: Option[ActionResult] = if (f()) Some(Yes) else Some(No)
  override def name: String = "Branch"
}

class SplitTaskDefinition extends TaskDefinition {
  override def action: Option[ActionResult] = Some(Ok)
  override def name: String = "Split"
}

object StartTaskDefinition extends TaskDefinition {
  override def action: Option[ActionResult] = Option(Ok)
  override def name: String = "Start"
}

object EndTaskDefinition extends TaskDefinition {
  override def action: Option[ActionResult] = Option(Ok)
  override def name: String = "End"
}

class JoinTaskDefinition(n: Int) extends TaskDefinition {
  var inputLines = n

  override def action: Option[ActionResult] = {
    inputLines -= 1
    if (inputLines == 0)
      Some(Ok)
    else
      Some(JoinIsWaiting)
  }

  override def name: String = "Join"
}

final class Workflow(wfDef: WorkflowDefinition, parent: Option[Workflow]) {
  private val _tasks = mutable.ListBuffer.empty[Task]

  def this(wfDef: WorkflowDefinition) = this(wfDef, None)

  def start: Task = {
    val task = new Task(StartTaskDefinition, this)
    _tasks += task
    println("Started workflow \"%s\"".format(wfDef.name))
    println("Created task \"%s\"".format(task))
    task
  }

  def executeRound: List[Task] = {
    val newTasksHelper: mutable.ListBuffer[List[Task]] = for {
      t <- _tasks
      if !t.isExecuted
      r <- t.execute
      tDefs <- wfDef.transitions.get((t.taskDef, r))
      newTasks = for {
        tDef <- tDefs
        nt = new Task(tDef, this)
        _ = t.addChild(nt)
      } yield nt
    } yield newTasks
    val newTasks = newTasksHelper.toList.flatten
    newTasks foreach (t => _tasks += t)
    newTasks foreach (t => println("Created task \"%s\"".format(t)))
    newTasks
  }

  def isStarted: Boolean = _tasks.nonEmpty

  def allExecuted: Boolean = isStarted && (_tasks forall (t => t.isExecuted))

  def endExecuted: Boolean = isStarted && (_tasks exists (t => t.taskDef == EndTaskDefinition && t.isExecuted))
}

abstract class Service

object WorkflowCacheService extends Service {
  private var _intBag: Map[Workflow, mutable.Map[String, Int]] = Map()
  private var _stringBag: Map[Workflow, mutable.Map[String, String]] = Map()

  private def checkName(name: String): Boolean = {
    val r = for {
      bag <- List(_intBag, _stringBag)
      wfMap <- bag.values
      keyName <- wfMap.keys
      if name == keyName
    } yield name
    r.isEmpty
  }

  def put(wf: Workflow, name: String, value: Int): Unit = {
    require(checkName(name))
    _intBag.get(wf) match {
      case None => _intBag += (wf -> mutable.Map(name -> value))
      case Some(x) => x += (name -> value)
    }
  }

  def put(wf: Workflow, name: String, value: String): Unit = {
    require(checkName(name))
    _stringBag.get(wf) match {
      case None => _stringBag += (wf -> mutable.Map(name -> value))
      case Some(x) => x += (name -> value)
    }
  }

  def getInt(wf: Workflow, name: String): Int = {
    _intBag.get(wf).get(name)
  }

  def getString(wf: Workflow, name: String): String = {
    _stringBag.get(wf).get(name)
  }
}

object EngineService extends Service {
  def startWorkflow(wfDef: WorkflowDefinition): Workflow = Engine.startWorkflow(wfDef)
}

object Engine {
  val _workflows = mutable.ListBuffer.empty[Workflow]

  def startWorkflow(wfDef: WorkflowDefinition): Workflow = {
    startWorkflow(wfDef, None)
  }

  def startWorkflow(wfDef: WorkflowDefinition, parentWf: Option[Workflow]): Workflow = {
    val wf = new Workflow(wfDef, parentWf)
    wf.start
    _workflows += wf
    wf
  }

  def executeRound: Seq[Workflow] = for {
    wf <- _workflows
    if !wf.allExecuted
    dummy = wf.executeRound
  } yield wf
}


