package web

import scala.scalajs.js
import scala.scalajs.js.annotation._

@JSExportTopLevel("Compilation")
final case class Compilation(
  @JSExport parsed: StageResult[String],
  @JSExport translation: StageResult[js.Array[TranslationResult]],
  @JSExport types: StageResult[String],
  @JSExport target: js.UndefOr[StageResult[String]],
)

@JSExportTopLevel("ParsingInformation")
final case class StageResult[+T](
  @JSExport val content: js.UndefOr[T],
  @JSExport val reports: js.Array[Report],
)

object StageResult {
  def success[T](content: T): StageResult[T] = StageResult(content, js.Array())
  def failure(reports: js.Array[Report]): StageResult[Nothing] =
    StageResult(js.undefined, reports)
}
