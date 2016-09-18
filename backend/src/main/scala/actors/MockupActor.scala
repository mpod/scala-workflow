package actors

import akka.actor.Actor
import common.PublicActorMessages._
import common.Views._
import scala.util.Random
import scala.language.postfixOps

class MockupActor extends Actor {
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

  def receive = {
    case StartWorkflow(wfDefName, label) =>
      if (Random.nextBoolean())
        sender() ! Workflows(List(genWorkflowView))
      else
        sender() ! Error("Cannot start a workflow!")
    case GetWorkflowDefinitions =>
      sender() ! WorkflowDefinitions((1 to 5) map (_ => choice(workflowNames)))
    case GetWorkflows =>
      sender() ! Workflows(workflows)
    case ExecuteManualTask(wfRootId, wfId, taskId, fieldValues) =>
      if (Random.nextBoolean())
        sender() ! Workflows(List(genWorkflowView))
      else
        sender() ! Error("Cannot execute manual task!")
  }
}
