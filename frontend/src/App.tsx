import { useEffect, useState } from "react";
import { ApiError, getProblem, getProblems, submitSolution } from "./api/judgeApi";
import type { ProblemDetail, ProblemSummary, Submission } from "./types/api";
import { ProblemList } from "./components/ProblemList";
import { ProblemView } from "./components/ProblemView";

function buildStarterTemplate(methodSignature: string): string {
  return `public class Solution {
    ${methodSignature} {
        // Write your solution here
        return 0;
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

  useEffect(() => {
    if (selectedProblemId === null) return;

    let cancelled = false;

    getProblem(selectedProblemId)
      .then((detail) => {
        if (cancelled) return;
        setProblemDetail(detail);
        setSourceCode(buildStarterTemplate(detail.methodSignature));
      })
      .catch((error: unknown) => {
        if (!cancelled) setProblemLoadError(messageFor(error));
      });

    return () => {
      cancelled = true;
    };
  }, [selectedProblemId]);

  function handleSelectProblem(id: number) {
    setSelectedProblemId(id);
    setProblemDetail(null);
    setProblemLoadError(null);
    setSubmission(null);
    setSubmitError(null);
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
        <span className="app-title">Code Judge</span>
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
