package engine

class Cache {
  private var _intCache: Map[String, Int] = Map()
  private var _stringCache: Map[String, String] = Map()

  object ValueType extends Enumeration {
    val String, Int = Value
  }

  private def checkName(name: String, valueType: ValueType.Value): Boolean = {
    ! (valueType != ValueType.String && _stringCache.contains(name)) ||
      (valueType != ValueType.Int && _intCache.contains(name))
  }

  def setIntVal(name: String, value: Int): Unit = {
    require(checkName(name, ValueType.Int))
    _intCache += (name -> value)
  }

  def setStringVal(name: String, value: String): Unit = {
    require(checkName(name, ValueType.String))
    _stringCache += (name -> value)
  }

  def getIntVal(name: String): Int = {
    _intCache(name)
  }

  def getStringVal(name: String): String = {
    _stringCache(name)
  }

  def contains(name: String): Boolean = _intCache.contains(name) || _stringCache.contains(name)
}

