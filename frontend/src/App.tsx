import { useEffect, useRef, useState } from "react";
import { ApiError, getProblem, getProblems, submitSolution } from "./api/judgeApi";
import type { ProblemDetail, ProblemSummary, Submission } from "./types/api";
import { ProblemList } from "./components/ProblemList";
import { ProblemView } from "./components/ProblemView";

/**
 * Picks a compiling placeholder return value from the method signature's
 * return-type keyword (the third whitespace-separated token in
 * "public static <ReturnType> <name>(...)"). This is a targeted lookup of
 * one known keyword, not a Java type parser - it only needs to distinguish
 * boolean from everything else this judge currently supports.
 */
function defaultReturnStatement(methodSignature: string): string {
  const returnType = methodSignature.trim().split(/\s+/)[2];
  return returnType === "boolean" ? "return false;" : "return 0;";
}

function buildStarterTemplate(methodSignature: string): string {
  return `public class Solution {
    ${methodSignature} {
        // Write your solution here
        ${defaultReturnStatement(methodSignature)}
    }
}
`;
}

function messageFor(error: unknown): string {
  if (error instanceof ApiError) {
    return error.message;
  }
  return "Something went wrong.";
}

export default function App() {
  const [problems, setProblems] = useState<ProblemSummary[] | null>(null);
  const [problemsError, setProblemsError] = useState<string | null>(null);

  const [selectedProblemId, setSelectedProblemId] = useState<number | null>(null);
  const [problemDetail, setProblemDetail] = useState<ProblemDetail | null>(null);
  const [problemLoadError, setProblemLoadError] = useState<string | null>(null);

  const [sourceCode, setSourceCode] = useState("");
  const [submission, setSubmission] = useState<Submission | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;

    getProblems()
      .then((loaded) => {
        if (!cancelled) setProblems(loaded);
      })
      .catch((error: unknown) => {
        if (!cancelled) setProblemsError(messageFor(error));
      });

    return () => {
      cancelled = true;
    };
  }, []);

  const latestRequestedProblemIdRef = useRef<number | null>(null);

  async function handleSelectProblem(id: number) {
    latestRequestedProblemIdRef.current = id;
    setSelectedProblemId(id);
    setProblemDetail(null);
    setProblemLoadError(null);
    setSubmission(null);
    setSubmitError(null);

    try {
      const detail = await getProblem(id);
      if (latestRequestedProblemIdRef.current !== id) return;
      setProblemDetail(detail);
      setSourceCode(buildStarterTemplate(detail.methodSignature));
    } catch (error) {
      if (latestRequestedProblemIdRef.current !== id) return;
      setProblemLoadError(messageFor(error));
    }
  }

  async function handleSubmit() {
    if (selectedProblemId === null) return;

    setSubmitting(true);
    setSubmitError(null);
    setSubmission(null);

    try {
      const result = await submitSolution(selectedProblemId, sourceCode);
      setSubmission(result);
    } catch (error) {
      setSubmitError(messageFor(error));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="app-layout">
      <header className="app-header">
        <span className="app-title">Not L**tcode</span>
        <div className="app-meta">
          <span>Java 25</span>
          <span>Local executor</span>
        </div>
      </header>
      <div className="app-body">
        <ProblemList
          problems={problems}
          error={problemsError}
          selectedId={selectedProblemId}
          onSelect={handleSelectProblem}
        />
        <ProblemView
          problem={problemDetail}
          loadError={problemLoadError}
          sourceCode={sourceCode}
          onSourceChange={setSourceCode}
          onSubmit={handleSubmit}
          submitting={submitting}
          submission={submission}
          submitError={submitError}
        />
      </div>
    </div>
  );
}
