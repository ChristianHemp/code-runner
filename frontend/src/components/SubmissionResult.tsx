import type { Submission, Verdict } from "../types/api";

interface SubmissionResultProps {
  submission: Submission | null;
  submitting: boolean;
  error: string | null;
}

const VERDICT_LABELS: Record<Verdict, string> = {
  ACCEPTED: "Accepted",
  WRONG_ANSWER: "Wrong Answer",
  COMPILATION_ERROR: "Compilation Error",
  RUNTIME_ERROR: "Runtime Error",
  TIME_LIMIT_EXCEEDED: "Time Limit Exceeded",
};

const VERDICT_CLASSES: Record<Verdict, string> = {
  ACCEPTED: "verdict-accepted",
  WRONG_ANSWER: "verdict-rejected",
  COMPILATION_ERROR: "verdict-rejected",
  RUNTIME_ERROR: "verdict-rejected",
  TIME_LIMIT_EXCEEDED: "verdict-warning",
};

export function SubmissionResult({ submission, submitting, error }: SubmissionResultProps) {
  if (submitting) {
    return (
      <div className="submission-result">
        <p className="muted">Judging…</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="submission-result">
        <p className="error-text">{error}</p>
      </div>
    );
  }

  if (!submission) {
    return null;
  }

  const verdict = submission.verdict;

  return (
    <div className={verdict ? `submission-result ${VERDICT_CLASSES[verdict]}` : "submission-result"}>
      <div className="verdict-line">
        <span className="verdict-word">{verdict ? VERDICT_LABELS[verdict] : submission.status}</span>
        {submission.runtimeMs !== null && (
          <span className="verdict-runtime">{submission.runtimeMs} ms</span>
        )}
      </div>
      {submission.errorMessage && (
        <div className="submission-error">
          <span className="field-label">Details</span>
          <pre>{submission.errorMessage}</pre>
        </div>
      )}
    </div>
  );
}
