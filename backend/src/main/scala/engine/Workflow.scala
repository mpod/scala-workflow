package engine

import com.typesafe.scalalogging.LazyLogging
import engine.TaskDefinition.{EndTaskDefinition, StartTaskDefinition}

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

  def findTask(taskId: Int): Option[Task] = _tasks.find(_.id == taskId)
}

