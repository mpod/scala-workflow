# scala-workflow #

Experimental workflow engine written in Scala. 

## Features ##

* Built upon Akka 
* Uses Play for frontend side
* Activities can transition into multiple new activities in parallel
* _Join_ activity for merging branches into one
* _Subflow_ activity enables workflow compositions
* _Manual task_ activity for collecting input data
* Workflows are designed in Scala code as a set of activities and transitions

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

```scala
object Demo extends WorkflowDefinition {
  val man1 = new ManualTaskDefinition(List(
    IntField("Simple Int field", "intfield")
  ))
  val man2 = new ManualTaskDefinition(List(
    StringField("Simple String field", "strfield")
  ))
  val branch = new BranchTaskDefinition(context => {
    context.task.parent.get.value.get[Int]("intfield").get > 100
  })
  val subflow = new SubWorkflowTaskDefinition(Demo)
  val join1 = new JoinTaskDefinition(Set(subflow, branch), true)
  val join2 = new JoinTaskDefinition(Set(join1, man2))

  override val transitions: Map[(TaskDefinition, ActionResult), List[TaskDefinition]] = Map(
    (StartTaskDefinition, Ok) -> List(man1, man2),
    (man1, Ok) -> List(branch),
    (branch, Yes) -> List(subflow),
    (branch, No) -> List(join1),
    (subflow, Ok) -> List(join1),
    (join1, Ok) -> List(join2),
    (man2, Ok) -> List(join2),
    (join2, Ok) -> List(EndTaskDefinition)
  )
  override val name: String = "Demo"
}
```

