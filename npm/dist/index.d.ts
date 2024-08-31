declare module "@mlscript/ucs-demo-build" {
  export function compile(source: string): Compilation;

  export type Report = DiagnosticReport | FatalErrorReport;
  export type DiagnosticReport = {
    kind: "error" | "warning";
    messages: DiagnosticReportMessage[];
  };
  export type DiagnosticReportMessage =
    | { kind: "text"; content: string }
    | { kind: "code"; line: number; content: string; range: [number, number] };
  export type FatalErrorReport = {
    kind: "fatal";
    message: string;
    stack: string[];
  };

  export class StageResult<T> {
    public content?: T;
    public reports: Report[];
  }
  export class Compilation {
    public parsed: StageResult<string>;
    public translation: StageResult<TranslationResult[]>;
    public types: StageResult<string>;
    public target?: StageResult<string>;
  }

  // Types for translation results
  export class TranslationResult {
    public locations: [number, number] | undefined;
    public transformed: SourceSplit;
    public desugared: CoreSplit;
    public normalized: NormalizedTerm;
    public postProcessed: NormalizedTerm;
    public coverageCheckingResults: DiagnosticReport[];
  }

  // Types for unserialized terms.
  export type MLscriptTerm = { type: "Term"; term: string };

  // Types for source abstract syntax terms.

  export type SourceSplit =
    | { type: "Cons"; head: SourceBranch; tail: SourceSplit }
    | {
        type: "Let";
        rec: boolean;
        name: string;
        rhs: MLscriptTerm;
        tail: SourceSplit;
      }
    | { type: "Else"; term: MLscriptTerm }
    | { type: "Nil" };

  export type SourceBranch = (
    | { type: "OperatorBranch.Binary"; operator: string }
    | { type: "OperatorBranch.Match"; operator: string }
    | { type: "PatternBranch.Pattern"; pattern: SourcePattern }
    | { type: "TermBranch.Boolean"; test: MLscriptTerm }
    | { type: "TermBranch.Left"; left: MLscriptTerm }
    | { type: "TermBranch.Match"; scrutinee: MLscriptTerm }
  ) & { continuation: SourceSplit };

  export type SourcePattern =
    | { type: "Record"; fields: { name: string; pattern: SourcePattern }[] }
    | { type: "Concrete"; name: string }
    | { type: "Alias"; name: string; pattern: SourcePattern }
    | { type: "Empty"; source: MLscriptTerm }
    | { type: "Tuple"; fields: SourcePattern[] }
    | {
        type: "Class";
        name: string;
        parameters: SourcePattern[] | undefined;
        refined: boolean;
      }
    | { type: "Literal"; literal: MLscriptTerm }
    | { type: "Name"; name: string };

  // Types for core abstract syntax terms.

  export type CoreSplit =
    | { type: "Cons"; head: CoreBranch; tail: CoreSplit }
    | { type: "Let"; rec: boolean; name: string; rhs: CoreTerm; tail: CoreTerm }
    | { type: "Else"; term: MLscriptTerm }
    | { type: "Nil" };

  export type CoreBranch = {
    type: "Branch";
    scrutinee: string;
    pattern: CorePattern;
    continuation: CoreSplit;
  };

  export type CorePattern =
    | {
        type: "Class";
        name: string;
        symbol: string;
        originallyRefined: boolean;
      }
    | { type: "Literal"; literal: MLscriptTerm }
    | { type: "Name"; name: string };

  // Types for normalized terms.

  export type NormalizedTerm =
    | MLscriptTerm
    | { type: "CaseOf"; scrutinee: MLscriptTerm; cases: NormalizedTermCase }
    | {
        type: "Let";
        isRec: boolean;
        name: string;
        rhs: MLscriptTerm;
        body: NormalizedTerm;
      };

  export type NormalizedTermCase =
    | {
        type: "Case";
        refined: boolean;
        pattern: NormalizedTermCasePattern;
        rhs: NormalizedTerm;
        tail: NormalizedTermCase;
      }
    | { type: "Wildcard"; term: NormalizedTerm }
    | { type: "NoCases" };

  export type NormalizedTermCasePattern =
    | { type: "Constructor"; name: string }
    | { type: "Other"; term: MLscriptTerm };
}
