package web

import scalajs.js, js.annotation._, js.JSConverters._
import mlscript.{Case, CaseBranches, CaseOf, Let, NoCases, SimpleTerm, Term, Wildcard, Var}
import mlscript.ucs.syntax.{source => s, core => c}
import mlscript.ucs.context.Context
import mlscript.utils._, shorthands._

object Serialization {
  def serializeNormalizedTerm(term: Term)(implicit context: Context): js.Dynamic = {
    def showTerm(term: Term): js.Dynamic = term.desugaredTerm match {
      case N => term match {
        case let: Let => showLet(let)
        case caseOf: CaseOf => showCaseOf(caseOf)
        case other => js.Dynamic.literal("type" -> "Term", "term" -> other.showDbg)
      }
      case S(desugared) => desugared.toJSObject
    }
    def showPattern(pat: SimpleTerm): js.Dynamic = pat match {
      case vari: Var => js.Dynamic.literal("type" -> "Constructor", "name" -> vari.name)
      case _ => js.Dynamic.literal("type" -> "Other", "term" -> pat.toJSObject)
    }
    def showCaseOf(caseOf: CaseOf): js.Dynamic =
      js.Dynamic.literal(
        "type" -> "CaseOf",
        "scrutinee" -> caseOf.trm.toJSObject,
        "cases" -> showCaseBranches(caseOf.cases))
    def showCaseBranches(caseBranches: CaseBranches): js.Dynamic =
      caseBranches match {
        case k @ Case(pat, rhs, tail) =>
          js.Dynamic.literal(
            "type" -> "Case",
            "refined" -> k.refined,
            "pattern" -> showPattern(pat),
            "rhs" -> showTerm(rhs),
            "tail" -> showCaseBranches(tail))
        case Wildcard(term) =>
          js.Dynamic.literal("type" -> "Wildcard", "term" -> showTerm(term))
        case NoCases => js.Dynamic.literal("type" -> "NoCases")
      }
    def showLet(let: Let): js.Dynamic =
      js.Dynamic.literal(
        "type" -> "Let",
        "isRec" -> let.isRec, 
        "name" -> let.name.name, 
        "rhs" -> let.rhs.showDbg, 
        "body" -> showTerm(let.body))
    showTerm(term)
  }

  implicit class TermSerializer(val self: Term) extends AnyVal {
    def toJSObject: js.Dynamic = js.Dynamic.literal("type" -> "Term", "term" -> self.showDbg)
  }

  implicit class SourceSplitSerializer[B <: s.Branch](val self: s.Split[B]) extends AnyVal {
    def toJSObject: js.Dynamic =
      self match {
        case s.Split.Cons(head, tail) =>
          js.Dynamic.literal("type" -> "Cons", "head" -> head.toJSObject, "tail" -> tail.toJSObject)
        case s.Split.Let(rec, nme, rhs, tail) =>
          js.Dynamic.literal("type" -> "Let", "rec" -> rec, "nme" -> nme.name, "rhs" -> rhs.toJSObject, "tail" -> tail.toJSObject)
        case s.Split.Else(term) => js.Dynamic.literal("type" -> "Else", "term" -> term.toJSObject)
        case s.Split.Nil => js.Dynamic.literal("type" -> "Nil")
      }
  }

  implicit class SourceBranchSerializer(val self: s.Branch) extends AnyVal {
    def toJSObject: js.Dynamic =
      self match {
        case s.OperatorBranch.Binary(operator, continuation) => 
          js.Dynamic.literal("type" -> "OperatorBranch.Binary", "operator" -> operator.name, "continuation" -> continuation.toJSObject)
        case s.OperatorBranch.Match(operator, continuation) =>
          js.Dynamic.literal("type" -> "OperatorBranch.Match", "operator" -> operator.name, "continuation" -> continuation.toJSObject)
        case s.PatternBranch(pattern, continuation) =>
          js.Dynamic.literal("type" -> "PatternBranch.Pattern", "pattern" -> pattern.toJSObject, "continuation" -> continuation.toJSObject)
        case s.TermBranch.Boolean(test, continuation) =>
          js.Dynamic.literal("type" -> "TermBranch.Boolean", "test" -> test.toJSObject, "continuation" -> continuation.toJSObject)
        case s.TermBranch.Left(left, continuation) =>
          js.Dynamic.literal("type" -> "TermBranch.Left", "left" -> left.toJSObject, "continuation" -> continuation.toJSObject)
        case s.TermBranch.Match(scrutinee, continuation) =>
          js.Dynamic.literal("type" -> "TermBranch.Match", "scrutinee" -> scrutinee.toJSObject, "continuation" -> continuation.toJSObject)
      }
  }

  implicit class SourcePatternSerializer(val self: s.Pattern) extends AnyVal {
    def toJSObject: js.Dynamic = self match {
      case s.RecordPattern(entries) =>
        js.Dynamic.literal("type" -> "Record", "entries" -> entries.map { case (nme, pattern) =>
          js.Dynamic.literal("name" -> nme.name, "pattern" -> pattern.toJSObject)
        }.toJSArray)
      case s.ConcretePattern(nme) =>
        js.Dynamic.literal("type" -> "Concrete", "name" -> nme.name)
      case s.AliasPattern(nme, pattern) =>
        js.Dynamic.literal("type" -> "Alias", "name" -> nme.name, "pattern" -> pattern.toJSObject)
      case s.EmptyPattern(source) =>
        js.Dynamic.literal("type" -> "Empty", "source" -> source.toJSObject)
      case s.TuplePattern(fields) =>
        js.Dynamic.literal("type" -> "Tuple", "fields" -> fields.map(_.toJSObject).toJSArray)
      case s.ClassPattern(nme, parameters, refined) =>
        js.Dynamic.literal("type" -> "Class", "name" -> nme.name, "parameters" -> parameters.map(_.map(_.toJSObject).toJSArray).getOrElse(js.undefined), "refined" -> refined)
      case s.LiteralPattern(literal) =>
        js.Dynamic.literal("type" -> "Literal", "literal" -> literal.toJSObject)
      case s.NamePattern(nme) =>
        js.Dynamic.literal("type" -> "Name", "name" -> nme.name)
    }
  }

  implicit class CoreSplitSerializer(val self: c.Split) extends AnyVal {
    def toJSObject: js.Dynamic =
      self match {
        case c.Split.Cons(head, tail) =>
          js.Dynamic.literal("type" -> "Cons", "head" -> head.toJSObject, "tail" -> tail.toJSObject)
        case c.Split.Let(rec, nme, rhs, tail) =>
          js.Dynamic.literal("type" -> "Let", "rec" -> rec, "nme" -> nme.name, "rhs" -> rhs.toJSObject, "tail" -> tail.toJSObject)
        case c.Split.Else(term) => js.Dynamic.literal("type" -> "Else", "term" -> term.toJSObject)
        case c.Split.Nil => js.Dynamic.literal("type" -> "Nil")
      }
  }

  implicit class CoreBranchSerializer(val self: c.Branch) extends AnyVal {
    def toJSObject: js.Dynamic = js.Dynamic.literal(
      "type" -> "Branch",
      "scrutinee" -> self.scrutinee.name,
      "pattern" -> self.pattern.toJSObject,
      "continuation" -> self.continuation.toJSObject
    )
  }

  implicit class CorePatternSerializer(val self: c.Pattern) extends AnyVal {
    def toJSObject: js.Dynamic = self match {
      case c.Pattern.Class(nme, symbol, originallyRefined) => 
        js.Dynamic.literal("type" -> "Class", "name" -> nme.name, "symbol" -> symbol.name, "originallyRefined" -> originallyRefined)
      case c.Pattern.Literal(literal) => js.Dynamic.literal("type" -> "Literal", "literal" -> literal.toJSObject)
      case c.Pattern.Name(nme) => js.Dynamic.literal("type" -> "Name", "name" -> nme.name)
    }
  }
}