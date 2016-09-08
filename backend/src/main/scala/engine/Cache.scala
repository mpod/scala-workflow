package engine

trait Cache {

  private var _cache: Map[String, Any] = Map()

  def put[T](name: String, value: T) = _cache += (name -> value)

  def get[T](name: String): T = _cache(name).asInstanceOf[T]

  def contains(name: String): Boolean = _cache contains name
}

