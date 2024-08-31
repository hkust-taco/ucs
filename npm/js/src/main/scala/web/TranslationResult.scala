package web

import scalajs.js, js.annotation._

@JSExportTopLevel("TranslationResult")
final case class TranslationResult(
  @JSExport locations: js.UndefOr[js.Tuple2[Int, Int]],
  @JSExport transformed: js.Dynamic,
  @JSExport desugared: js.Dynamic,
  @JSExport normalized: js.Dynamic,
  @JSExport postProcessed: js.Dynamic,
  @JSExport coverageCheckingResults: js.Array[js.Dynamic],
)
