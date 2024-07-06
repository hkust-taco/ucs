package web

import scala.scalajs.js, js.annotation._, js.JSConverters._
import mlscript.{NewLexer, NewParser, ShowCtx, NewJSWebBackend}
import mlscript.{TypingUnit, Pgrm}
import mlscript.{Diagnostic, WarningReport, ErrorReport, Message, Raise}
import mlscript.pretyper.Scope
import mlscript.utils._, shorthands._
import scala.collection.mutable.ArrayBuffer

object WebDemo {
  @JSExportTopLevel("compile")
  def compile(source: String, prelude: String): Compilation = {
    val blockLineNum = 0
    val showRelativeLineNums = false
    
    def reportDiagnostic(diag: Diagnostic): js.Dynamic = {
      val sctx = Message.mkCtx(diag.allMsgs.iterator.map(_._1), newDefs=true, "?")
      val kind = diag match {
        case ErrorReport(msg, loco, src) => "error"
        case WarningReport(msg, loco, src) => "warning"
      }
      val buffer = new ArrayBuffer[js.Dynamic]()
      val lastMsgNum = diag.allMsgs.size - 1
      /** solely used for reporting useful test failure messages */
      var globalLineNum = blockLineNum
      diag.allMsgs.zipWithIndex.foreach { case ((msg, loco), msgNum) =>
        val isLast = msgNum =:= lastMsgNum
        val msgStr = msg.showIn(sctx)
        buffer += js.Dynamic.literal("kind" -> "text", "content" -> msgStr)
        loco.foreach { loc =>
          val (startLineNum, startLineStr, startLineCol) =
            loc.origin.fph.getLineColAt(loc.spanStart)
          if (globalLineNum =:= 0) globalLineNum += startLineNum - 1
          val (endLineNum, endLineStr, endLineCol) =
            loc.origin.fph.getLineColAt(loc.spanEnd)
          var l = startLineNum
          var c = startLineCol // c starts from 1
          while (l <= endLineNum) {
            val globalLineNum = loc.origin.startLineNum + l - 1
            val relativeLineNum = globalLineNum - blockLineNum + 1
            val lineNum = if (showRelativeLineNums && relativeLineNum > 0)
              relativeLineNum else globalLineNum
            val curLine = loc.origin.fph.lines(l - 1)
            val lastCol =
              if (l =:= endLineNum) endLineCol else curLine.length + 1
            buffer += js.Dynamic.literal(
              "kind" -> "code",
              "line" -> lineNum,
              "content" -> curLine,
              "range" -> js.Tuple2(c - 1, lastCol - 1)
            )
            c = 1
            l += 1
          }
        }
      }
      js.Dynamic.literal(
        "kind" -> kind,
        "messages" -> buffer.toJSArray
      )
    }

    def reportError(e: Throwable): js.Dynamic = js.Dynamic.literal(
      "kind" -> "fatal",
      "message" -> e.getMessage,
      "stack" -> e.getStackTrace.map(_.toString).toJSArray
    )

    val buffer = new ArrayBuffer[js.Dynamic]()
    val parsingResult = try {
      import fastparse._
      import fastparse.Parsed.{Success, Failure}
      import mlscript.{NewParser, ErrorReport, Origin}
      val lines = source.splitSane('\n').toIndexedSeq
      val processedBlock = lines.mkString
      val fph = new mlscript.FastParseHelpers(source, lines)
      val origin = Origin("<input>", 1, fph)
      val lexer = new NewLexer(origin, buffer += _ |> reportDiagnostic, dbg = false)
      val tokens = lexer.bracketedTokens
      val parser = new NewParser(origin, tokens, newDefs = true, buffer += _ |> reportDiagnostic, dbg = false, N) {
        def doPrintDbg(msg: => Str): Unit = if (dbg) println(msg)
      }
      val tu = parser.parseAll(parser.typingUnit)
      (tu, Pgrm(tu.entities)): js.UndefOr[(TypingUnit, Pgrm)]
    } catch {
      case err: Diagnostic => buffer += reportDiagnostic(err); js.undefined
      case err: Throwable => buffer += reportError(err); js.undefined
    }
    val parsed = StageResult(
      parsingResult.map(_._1.toString()),
      buffer.toJSArray
    )

    val translated = parsingResult.fold(
      StageResult[js.Array[TranslationResult]](js.undefined, js.Array())
    ){ case (tu, pgrm) => 
      try {
        val preTyper = new PreTyper {
          override def debugTopicFilters = N
          override protected def emitString(str: Str): Unit = ()
        }
        preTyper(tu, Scope.global, "<root>")
        StageResult.success(preTyper.translationResults.toJSArray)
      } catch {
        case err: Diagnostic =>
          StageResult.failure(js.Array(reportDiagnostic(err)))
        case err: Throwable =>
          StageResult.failure(js.Array(reportError(err)))
      }
    }
    
    buffer.clear()
    val typingResult = parsingResult.flatMap { case (tu, pgrm) =>
      try {
        println(s"Parsed: $pgrm")
        val typer = new mlscript.Typer(
          dbg = false, verbose = false, explainErrors = false, newDefs = true)        
        import typer._

        implicit val raise: Raise = buffer += _ |> reportDiagnostic
        implicit var ctx: Ctx = Ctx.init
        implicit val extrCtx: Opt[typer.ExtrCtx] = N

        val vars: Map[Str, typer.SimpleType] = Map.empty
        val tpd = typer.typeTypingUnit(tu, N)(ctx.nest, raise, vars)
        object SimplifyPipeline extends typer.SimplifyPipeline {
          def debugOutput(msg: => Str): Unit = println(msg)
        }
        val sim = SimplifyPipeline(tpd, S(true))(ctx)
        val exp = typer.expandType(sim)(ctx)
        exp.showIn(0)(ShowCtx.mk(exp :: Nil, newDefs = true)): js.UndefOr[String]
      } catch {
        case err: Diagnostic => buffer += reportDiagnostic(err); js.undefined
        case err: Throwable => buffer += reportError(err); js.undefined
      }
    }
    val types = StageResult(typingResult, buffer.toJSArray)

    val generated = parsingResult.map { case (tu, pgrm) =>
      try {
        val backend = new NewJSWebBackend()
        val (lines, resNames) = backend(pgrm)
        StageResult.success(lines.mkString("\n"))
      } catch {
        case err: Throwable => StageResult.failure(js.Array(reportError(err)))
      }
    }

    Compilation(parsed, translated, types, generated)
  }
}
