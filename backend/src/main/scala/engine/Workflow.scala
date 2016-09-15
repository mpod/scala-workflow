package engine

import com.typesafe.scalalogging.LazyLogging
import engine.TaskDefinition.{EndTaskDefinition, StartTaskDefinition}

final class Workflow(val id: Int, wfDef: WorkflowDefinition, val label: String, parent: Option[Workflow])
  extends Cache with LazyLogging {

  private var _tasks = List.empty[Task]

  def tasks = _tasks

  def workflowDef = wfDef

  def parentWorkflow = parent

  def this(id: Int, wfDef: WorkflowDefinition, label: String) = this(id, wfDef, label, None)

  def start(implicit engine: Engine): Task = {
    val task = new Task(engine.idGenerator.nextId, StartTaskDefinition, this)
    _tasks ::= task
    logger.debug("Started workflow \"%s\" with id %d".format(wfDef.name, id))
    task
  }

  def executeRound(implicit engine: Engine): List[Task] = for {
    t <- _tasks
    if !t.isExecuted
    r = t.execute
    if r.isDefined
    tDef <- wfDef.transitions.getOrElse((t.taskDef, r.get), List.empty[TaskDefinition])
  } yield {
    val nt = new Task(engine.idGenerator.nextId, tDef, this)
    t.addChild(nt)
    _tasks ::= nt
    nt
  }

  def findTask(taskId: Int): Option[Task] = tasks find (_.id == taskId)

  def setManualTaskFields(taskId: Int, values: Map[String, String]): Unit = {
    findTask(taskId) match {
      case Some(t) =>
        t.taskDef match {
          case td: ManualTaskDefinition => values.foreach({
            case (k, v) => td.setField(k, v)(t)
          })
          case _ => throw new IllegalArgumentException("Task is not manual.")
        }
      case None => throw new IllegalArgumentException("Manual task not found.")
    }
  }

  def getManualTaskFields(taskId: Int): Seq[ManualTaskDefinition.Field] = {
    findTask(taskId) match {
      case Some(t) => t.taskDef match {
        case td: ManualTaskDefinition => td.fieldsMap.values.toSeq
        case _ => throw new IllegalArgumentException("Task is not manual.")
      }
      case None => throw new IllegalArgumentException("Manual task not found.")
    }
  }

  def isStarted: Boolean = _tasks.nonEmpty

  def allExecuted: Boolean = isStarted && (_tasks forall (t => t.isExecuted))

  def endExecuted: Boolean = isStarted && (_tasks exists (t => t.taskDef == EndTaskDefinition && t.isExecuted))

}

