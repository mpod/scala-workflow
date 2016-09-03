package engine

import engine.ActionResult.Ok
import engine.Task.TaskActionContext

object ManualTaskDefinition {
  abstract class Field {
    type ValueType
    val label: String
    val name: String
    def value_=(value: ValueType)(implicit context: TaskActionContext)
    def value(implicit context: TaskActionContext): Option[ValueType]
    def isSet(implicit context: TaskActionContext) = context.task.cache.contains(name)
    def typeName: String
  }

  case class StringField(label: String, name: String) extends Field {
    type ValueType = String
    override def value_=(value: ValueType)(implicit context: TaskActionContext): Unit = {
      context.task.cache.setStringVal(name, value)
    }
    override def value(implicit context: TaskActionContext): Option[String] =
      Option(context.task.cache.getStringVal(name))
    override def typeName = "String"
  }

  case class IntField(label: String, name: String) extends Field {
    type ValueType = Int
    override def value_=(value: ValueType)(implicit context: TaskActionContext): Unit = {
      context.task.cache.setIntVal(name, value)
    }
    override def value(implicit context: TaskActionContext): Option[Int] =
      Option(context.task.cache.getIntVal(name))
    override def typeName = "Int"
  }
}

class ManualTaskDefinition(val fields: List[ManualTaskDefinition.Field]) extends TaskDefinition {
  import ManualTaskDefinition._
  val fieldsMap: Map[String, Field] = fields.map(_.name).zip(fields).toMap

  override def action(context: TaskActionContext): Option[ActionResult] = {
    if (allFieldsSet(context)) Some(Ok) else None
  }

  override def name: String = "Manual"

  def allFieldsSet(implicit context: TaskActionContext) = fields.forall(_.isSet)

  def setField(name: String, value: Any)(implicit context: TaskActionContext) = (fieldsMap.get(name), value) match {
    case (Some(f: IntField), v: Int) => f.value_=(v)
    case (Some(f: StringField), v: String) => f.value_=(v)
    case (Some(f), _) =>
      throw new IllegalArgumentException(
        "Field %s is of type %s, while given value is of type %s".format(
          name, f.getClass.getName, value.getClass.getName
        )
      )
    case (None, _) =>
      throw new IllegalArgumentException("Field %s not found.".format(name))
  }
}
