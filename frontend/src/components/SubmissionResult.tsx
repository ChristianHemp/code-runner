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
    <div className="submission-result">
      <div className={verdict ? `verdict-badge ${VERDICT_CLASSES[verdict]}` : "verdict-badge"}>
        {verdict ? VERDICT_LABELS[verdict] : submission.status}
      </div>
      <dl className="submission-meta">
        {submission.runtimeMs !== null && (
          <div className="submission-meta-row">
            <dt>Runtime</dt>
            <dd>{submission.runtimeMs} ms</dd>
          </div>
        )}
        {submission.errorMessage && (
          <div className="submission-meta-row submission-error">
            <dt>Details</dt>
            <dd>
              <pre>{submission.errorMessage}</pre>
            </dd>
          </div>
        )}
      </dl>
    </div>
  );
}
