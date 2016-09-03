import actors.IdAllocatorActor
import actors.PrivateActorMessages.{ExecuteRound, IdAllocatorActorRef}
import akka.actor.{Actor, ActorSystem, Props}
import common.PublicActorMessages._
import common.Views._
import engine.{Engine, IdGenerator}
import engine.ImplicitConversions._

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Random
import scala.concurrent.ExecutionContext.Implicits.global

class Mockup extends Actor {
  val fieldNames = List("Proin", "Etiam", "Integer", "Donec", "Sed", "Aliquam", "Nam", "Vivamus", "Aliquam")
  val fieldLabels = List(
    "Proin iaculis nisi eu nisi lobortis",
    "Etiam cursus libero sed diam volutpat",
    "Integer eget sem ut libero imperdiet pretium.",
    "Donec et nisi sed tortor commodo auctor.",
    "Sed et quam semper, pellentesque ex non, molestie sem.",
    "Aliquam dapibus tortor eget imperdiet molestie.",
    "Nam finibus lorem quis suscipit sagittis.",
    "Vivamus vitae diam sodales, maximus ex quis, placerat leo.",
    "Aliquam sit amet arcu blandit, posuere sapien at, volutpat neque."
  )
  val stringFieldValues = List("Praesent quis mi eu.", "Aliquam blandit faucibus dictum.", "Quisque ut lacinia justo.",
    "Praesent ultrices scelerisque augue.", "Sed ac dapibus augue.", "Nullam massa erat, bibendum.")
  val taskStates = List("Open", "Closed")
  val taskDefNames = List("Interdum", "Pede", "Malesuada", "Amet", "Nunc", "Sit", "Adipiscing", "Curabitur",
    "Eros", "Eu", "Quam", "Egestas", "Vivamus", "Sed", "Reprehenderit")
  val workflowStates = List("Waiting on manual task", "Finished", "Running")
  val workflowNames = List(
    "Proin iaculis nisi eu nisi lobortis",
    "Etiam cursus libero sed diam volutpat",
    "Integer eget sem ut libero imperdiet pretium.",
    "Donec et nisi sed tortor commodo auctor.",
    "Sed et quam semper, pellentesque ex non, molestie sem.",
    "Aliquam dapibus tortor eget imperdiet molestie.",
    "Nam finibus lorem quis suscipit sagittis."
  )

  def choice[T](list: List[T]) = list(Random.nextInt(list.length))

  def genManualTaskStringFieldView = {
    val name = choice(fieldNames)
    val label = choice(fieldLabels)
    val value = if (Random.nextBoolean()) Some(choice(stringFieldValues)) else None
    ManualTaskStringFieldView(name, label, value)
  }

  def genManualTaskIntFieldView = {
    val name = choice(fieldNames)
    val label = choice(fieldLabels)
    val value = if (Random.nextBoolean()) Some(Random.nextInt) else None
    ManualTaskIntFieldView(name, label, value)
  }

  def genManualTaskFieldView = {
    if (Random.nextBoolean())
      genManualTaskStringFieldView
    else
      genManualTaskIntFieldView
  }

  def genTaskView = {
    if (Random.nextBoolean()) {
      val fields = (1 to Random.nextInt(10)) map (_ => genManualTaskFieldView)
      ManualTaskView(Random.nextInt(100), choice(taskStates), choice(taskDefNames), fields)
    } else {
      TaskView(Random.nextInt(100), choice(taskStates), choice(taskDefNames))
    }
  }

  def genWorkflowView = {
    val tasks = (1 to Random.nextInt(20)) map (_ => genTaskView)
    WorkflowView(Random.nextInt(100), choice(workflowNames), choice(workflowNames), choice(workflowStates), tasks)
  }

  val workflows = (1 to 20) map (_ => genWorkflowView)

  implicit val idGen = IdGenerator.SimpleIdGenerator

  val engine = new Engine()

  self ! ExecuteRound

  def receive = {
    case StartWorkflow(wfDefName, label) =>
      engine.workflowDefinitions.find(_.name == wfDefName) match {
        case Some(wfDef) =>
          val wf = engine.startWorkflow(wfDef, label)
          sender() ! Workflows(List(wf))
        case None => sender() ! Error("Invalid workflow definition name %s".format(wfDefName))
      }
    case GetWorkflowDefinitions =>
      sender() ! WorkflowDefinitions(engine.workflowDefinitions map (_.name))
    case GetWorkflows =>
      sender() ! Workflows(engine.workflows)
    case ExecuteRound =>
      engine.executeRound
      context.system.scheduler.scheduleOnce(1 second, self, ExecuteRound)
  }
}

object Backend extends App {
  val system = ActorSystem("workflows")
  val idAllocator = system.actorOf(Props[IdAllocatorActor], "allocator")
  val router = system.actorOf(Props[actors.RouterActor], "router")

  router ! IdAllocatorActorRef(idAllocator)

  //val mockupActor = system.actorOf(Props[Mockup], "mockup")

  Await.result(system.whenTerminated, Duration.Inf)
}
