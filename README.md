# scala-workflow #

Experimental workflow engine written in Scala. 

## Features ##

* Built upon Akka 
* Uses Play for frontend side
* Activities can transition into multiple new activities in parallel
* _Join_ activity for merging branches into one
* _Subflow_ activity enables workflow compositions
* _Manual task_ activity for collecting input data
* Workflows are designed in Scala code as a set of activities and transitions.

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



