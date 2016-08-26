import akka.actor.{Actor, ActorSystem, Props}
import common.PublicActorMessages.{CreateWorkflow, GetWorkflows, StartedWorkflow, Workflows}
import common.Views._

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.util.Random

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
    WorkflowView(Random.nextInt(100), tasks)
  }

  def receive = {
    case CreateWorkflow(wfDefName) =>
      sender() ! StartedWorkflow(wfDefName, 2)
    case GetWorkflows =>
      val workflows = (1 to Random.nextInt(20)) map (_ => genWorkflowView)
      sender() ! Workflows(workflows)
  }
}

object Backend extends App {
  val system = ActorSystem("workflows")

  val mockupActor = system.actorOf(Props[Mockup], "mockup")

  Await.result(system.whenTerminated, Duration.Inf)
}
