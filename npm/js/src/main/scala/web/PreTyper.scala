package web

import mlscript.pretyper.Scope
import mlscript.{If, Loc}
import mlscript.ucs.context.Context
import mlscript.ucs.display._
import mlscript.utils._, shorthands._
import scalajs.js, js.annotation._, js.JSConverters._
import collection.mutable.{Map => MutMap}

class PreTyper(convertDiagnostic: mlscript.Diagnostic => js.Dynamic) extends mlscript.pretyper.PreTyper {

  private val translationResultsMap = MutMap.empty[Context, TranslationResult]

  def translationResults: Iterator[TranslationResult] =
    translationResultsMap.valuesIterator

  /**
    * The entry-point of desugaring a UCS syntax tree (`If` node) to a normal
    * MLscript syntax tree made of `CaseOf` and `Let` nodes. `PreTyper` is
    * supposed to call this function. Note that the caller doesn't need to
    * resolve symbols and bindings inside the UCS tree.
    *
    * @param if the UCS syntax tree to be desugared
    * @param scope the scope of the `If` node
    */
  protected override def traverseIf(`if`: If)(implicit scope: Scope): Unit = {
    implicit val context: Context = new Context(`if`)
    trace("traverseIf") {
      // Stage 0: Transformation
      val transformed = traceWithTopic("transform") {
        println("STEP 0")
        transform(`if`)
      }
      traceWithTopic("transform.result") {
        println("Transformed UCS term:", withIndent = false)
        println(showSplit(transformed), withIndent = false)
      }
      // Stage 1: Desugaring
      val desugared = traceWithTopic("desugar") {
        println("STEP 1")
        desugar(transformed)
      }
      traceWithTopic("desugar.result") {
        println("Desugared UCS term:", withIndent = false)
        println(showSplit(desugared), withIndent = false)
      }
      traceWithTopic("traverse") {
        println("STEP 1.5")
        traverseSplit(desugared)
      }
      // Stage 2: Normalization
      val normalized = traceWithTopic("normalize") {
        println("STEP 2")
        normalize(desugared)
      }
      traceWithTopic("normalize.result") {
        println("Normalized UCS term:", withIndent = false)
        println(showNormalizedTerm(normalized), withIndent = false)
      }
      // Stage 3: Post-processing
      val postProcessed = traceWithTopic("postprocess") {
        println("STEP 3")
        postProcess(normalized)
      }
      traceWithTopic("postprocess.result") {
        println("Post-processed UCS term:", withIndent = false)
        println(showNormalizedTerm(postProcessed), withIndent = false)
      }
      // Stage 4: Coverage checking
      val coverageResults = traceWithTopic("coverage") {
        val checked = println("STEP 4")
        val diagnostics = checkCoverage(postProcessed)
        println(s"Coverage checking result: ${diagnostics.size} errors")
        raiseMany(diagnostics)
        diagnostics
      }
      traceWithTopic("desugared") {
        println(s"Desugared term: ${postProcessed.showDbg}", withIndent = false)
      }
      // Epilogue
      `if`.desugaredTerm = S(postProcessed)
      // Save the intermediate syntax trees.
      import Serialization._
      translationResultsMap += context -> TranslationResult(
        locations = `if`.toLoc match {
          case N => js.undefined
          case S(loc) => js.Tuple2(loc.spanStart, loc.spanEnd)
        },
        transformed = transformed.toJSObject,
        desugared = desugared.toJSObject,
        normalized = serializeNormalizedTerm(normalized),
        postProcessed = serializeNormalizedTerm(postProcessed),
        coverageCheckingResults = coverageResults.map(convertDiagnostic).toJSArray,
      )
    }(_ => "traverseIf ==> ()")
  }
}
