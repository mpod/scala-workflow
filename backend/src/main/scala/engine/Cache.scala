package engine

trait Cache {

  private var _cache: Map[String, Any] = Map()

  def put[T](name: String, value: T) = _cache += (name -> value)

  def get[T](name: String): Option[T] = {
    if (_cache contains name)
      Some(_cache(name).asInstanceOf[T])
    else
      None
  }

  def contains(name: String): Boolean = _cache contains name
}

