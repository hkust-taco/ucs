package web

import scala.scalajs.js
import scala.scalajs.js.annotation._

sealed abstract class  Report

@JSExportTopLevel("InternalReport")
final case class InternalReport(
  @JSExport kind: InternalReport.Kind,
  @JSExport messages: Array[InternalReport.Message]
) extends Report

object InternalReport {
  sealed abstract class Kind

  object Kind {
    @JSExportTopLevel("Error")
    final case object Error extends Kind

    @JSExportTopLevel("Warning")
    final case object Warning extends Kind
  }

  sealed abstract class Message

  object Message {
    @JSExportTopLevel("Text")
    final case class Text(text: String) extends Message

    @JSExportTopLevel("Code")
    final case class Code(
      lineNo: Int,
      code: String,
      emphasis: js.UndefOr[js.Tuple2[Int, Int]]
    ) extends Message
  }
}

@JSExportTopLevel("ExternalReport")
final case class ExternalReport(
  @JSExport message: String,
  @JSExport stackTraces: Array[String]
) extends Report
