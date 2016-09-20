package engine

abstract class ActionResult

object ActionResult {
  case object Ok extends ActionResult
  case object Yes extends ActionResult
  case object No extends ActionResult
  case object JoinIsWaiting extends ActionResult
}
