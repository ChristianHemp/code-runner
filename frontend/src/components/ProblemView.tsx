import type { ProblemDetail, Submission } from "../types/api";
import { CodeEditor } from "./CodeEditor";
import { SubmissionResult } from "./SubmissionResult";

interface ProblemViewProps {
  problem: ProblemDetail | null;
  loadError: string | null;
  sourceCode: string;
  onSourceChange: (value: string) => void;
  onSubmit: () => void;
  submitting: boolean;
  submission: Submission | null;
  submitError: string | null;
}

export function ProblemView({
  problem,
  loadError,
  sourceCode,
  onSourceChange,
  onSubmit,
  submitting,
  submission,
  submitError,
}: ProblemViewProps) {
  if (loadError) {
    return (
      <main className="problem-view">
        <p className="error-text">{loadError}</p>
      </main>
    );
  }

  if (!problem) {
    return (
      <main className="problem-view">
        <p className="muted">Select a problem to get started.</p>
      </main>
    );
  }

  return (
    <main className="problem-view">
      <h1>{problem.title}</h1>
      <p className="problem-description">{problem.description}</p>
      <pre className="method-signature">{problem.methodSignature}</pre>

      <CodeEditor value={sourceCode} onChange={onSourceChange} disabled={submitting} />

      <button type="button" className="submit-button" onClick={onSubmit} disabled={submitting}>
        {submitting ? "Judging…" : "Submit"}
      </button>

      <SubmissionResult submission={submission} submitting={submitting} error={submitError} />
    </main>
  );
}
