package mlscript.codegen

import mlscript._
import mlscript.utils._, shorthands._

object utils {
  implicit class LocatedOps(val t: Located) extends AnyVal {
    def freeVars2: Set[Var] = {
      def statements(stmts: Ls[Statement]): Set[Var] =
        stmts.iterator.foldRight(Set.empty[Var]) {
          case (NuFunDef(isLetRec, nme, _, _, L(rhs)), fvs) => fvs - nme ++ (isLetRec match {
            case N | S(true) => rhs.freeVars2 - nme
            case S(false) => rhs.freeVars2
          })
          case (td: NuTypeDef, fvs) =>
            if (td.kind === Mod || td.kind === Cls || td.kind === Trt)
              fvs - td.nameVar
            else
              fvs
          case (statement, fvs) => fvs ++ statement.freeVars2
        }
      val realOne = t match {
        case t: Term => t.desugaredTerm.getOrElse(t)
        case other => other
      }
      realOne match {
        // TypingUnit
        case TypingUnit(entities) => statements(entities)
        // Terms
        case v: Var => Set.single(v)
        case Lam(tup: Tup, body) => body.freeVars2 -- tup.freeVars2
        case App(lhs, rhs) => lhs.freeVars2 ++ rhs.freeVars2
        case Tup(fields) => fields.iterator.flatMap(_._2.value.freeVars2.iterator).toSet
        case Rcd(fields) => fields.iterator.flatMap(_._2.value.freeVars2.iterator).toSet
        case Sel(receiver, _) => receiver.freeVars2
        case Let(true, nme, rhs, body) => body.freeVars2 ++ rhs.freeVars2 - nme
        case Let(false, nme, rhs, body) => body.freeVars2 - nme ++ rhs.freeVars2
        case Blk(stmts) => statements(stmts)
        case Bra(_, trm) => trm.freeVars2
        case Asc(trm, _) => trm.freeVars2
        case Bind(lhs, rhs) => lhs.freeVars2 ++ rhs.freeVars2
        case Test(trm, _) => trm.freeVars2
        case With(trm, fields) => trm.freeVars2 ++ fields.freeVars2
        case CaseOf(trm, cases) => cases.foldLeft(trm.freeVars2)({
          case (fvs, kase -> body) => fvs ++ kase.freeVars2 ++ body.freeVars2
        })(_ ++ _.fold(Set.empty[Var])(_.freeVars2))
        case Subs(arr, idx) => arr.freeVars2 ++ idx.freeVars2
        case Assign(lhs, rhs) => lhs.freeVars2 ++ rhs.freeVars2
        case Splc(fields) => fields.iterator.flatMap(_.fold(_.freeVars2, _.value.freeVars2)).toSet
        case New(head, body) => head.fold(Set.empty[Var])(_._2.freeVars2) ++ body.freeVars2
        case NuNew(cls) => cls.freeVars2
        // Because `IfBody` uses the term to represent the pattern, direct
        // traversal is not correct.
        case If(_, _) => Set.empty
        case TyApp(lhs, _) => lhs.freeVars2
        case Where(body, where) => body.freeVars2 ++ statements(where)
        case Forall(_, body) => body.freeVars2
        case Inst(body) => body.freeVars2
        case Super() => Set.empty
        case Eqn(lhs, rhs) => lhs.freeVars2 ++ rhs.freeVars2
        case Rft(base, decls) => base.freeVars2 ++ decls.freeVars2
        // Fallback for unsupported terms which is incorrect most of the time.
        case _ => t.children.iterator.flatMap(_.freeVars2.iterator).toSet
      }
    }
  }
}