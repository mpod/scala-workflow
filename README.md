# scala-workflow #

Experimental workflow engine written in Scala. 

## Features ##

* Built upon Akka and Play
* Activities can transition into multiple new activities in parallel
* _Wait_ activity for merging branches
* _Subflow_ activity for workflow compositions
* _Manual task_ activity for collecting input data
* Custom activities

## Getting started ##

Clone repository to your computer.
```
$ git clone https://github.com/mpod/scala-workflow
$ cd scala-workflow
```

In first terminal execute.
```
$ sbt backend/run
```

In second terminal execute.
```
$ sbt frontened/run
```

Go to [http://localhost:9000](http://localhost:9000) and start few workflows.

## Example ##

Workflows are actually graphs that consist of activities and transitions between them. Following graph shows a workflow
with two parallel branches. Every branch contains a manual task. One branch contains conditional activity that drives
execution according to data entered in manual task. Subflow activity creates and executes another instance of the same
workflow. Wait nodes merge branches into one.

<img src="https://raw.github.com/mpod/scala-workflow/master/frontend/public/images/demo.png"/>

Here is an implementation of above workflow in format suitable for execution in the workflow engine.

```scala
object Demo extends WorkflowDefinition {
  val manual1 = new ManualTaskDefinition(List(
    IntField("Simple Int field", "intfield")
  ))
  val manual2 = new ManualTaskDefinition(List(
    StringField("Simple String field", "strfield")
  ))
  val branch = new BranchTaskDefinition(context => {
    context.task.parent.get.value.get[Int]("intfield").get > 100
  })
  val subflow = new SubWorkflowTaskDefinition(Demo)
  val wait1 = new WaitFirstTaskDefinition(subflow, branch)
  val wait2 = new WaitAllTaskDefinition(wait1, manual2)

  override val transitions: Map[(TaskDefinition, ActionResult), List[TaskDefinition]] = Map(
    (StartTaskDefinition, Ok) -> List(manual1, manual2),
    (manual1, Ok) -> List(branch),
    (branch, Yes) -> List(subflow),
    (branch, No) -> List(wait1),
    (subflow, Ok) -> List(wait1),
    (wait1, Ok) -> List(wait2),
    (manual2, Ok) -> List(wait2),
    (wait2, Ok) -> List(EndTaskDefinition)
  )
  override val name: String = "Demo"
}
```

To add new workflow definition to the list of available workflow definitions extend variable 
`availableWorkflowDefinitions` in file `backend/src/main/scala/definitions/package.scala`.
