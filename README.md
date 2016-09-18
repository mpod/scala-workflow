# scala-workflow #

Experimental workflow engine written in Scala. 

## Features ##

* Built upon Akka and Play
* Activities can transition into multiple new activities in parallel
* _Join_ activity for merging branches
* _Subflow_ activity for workflow compositions
* _Manual task_ activity for collecting input data

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

Go to [localhost:9000](http://localhost:9000) and start few workflows.

## Example ##

<img src="https://raw.github.com/mpod/scala-workflow/master/frontent/public/images/demo.svg"/>

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
  val join1 = new JoinTaskDefinition(Set(subflow, branch), waitOnlyForFirst = true)
  val join2 = new JoinTaskDefinition(Set(join1, manual2))

  override val transitions: Map[(TaskDefinition, ActionResult), List[TaskDefinition]] = Map(
    (StartTaskDefinition, Ok) -> List(manual1, manual2),
    (manual1, Ok) -> List(branch),
    (branch, Yes) -> List(subflow),
    (branch, No) -> List(join1),
    (subflow, Ok) -> List(join1),
    (join1, Ok) -> List(join2),
    (manual2, Ok) -> List(join2),
    (join2, Ok) -> List(EndTaskDefinition)
  )
  override val name: String = "Demo"
}
```

