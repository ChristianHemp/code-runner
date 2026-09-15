// Mirrors the backend DTOs exactly (see backend `problem`/`submission`/`execution`
// packages). Do not add fields the backend does not actually return.

export interface ProblemSummary {
  id: number;
  title: string;
}

export interface ProblemDetail {
  id: number;
  title: string;
  description: string;
  methodSignature: string;
}

export type SubmissionStatus = "PENDING" | "RUNNING" | "COMPLETED";

export type Verdict =
  | "ACCEPTED"
  | "WRONG_ANSWER"
  | "COMPILATION_ERROR"
  | "RUNTIME_ERROR"
  | "TIME_LIMIT_EXCEEDED";

export interface Submission {
  id: number;
  problemId: number;
  status: SubmissionStatus;
  verdict: Verdict | null;
  runtimeMs: number | null;
  errorMessage: string | null;
  submittedAt: string;
  completedAt: string | null;
}
