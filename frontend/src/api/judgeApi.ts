import type { ProblemDetail, ProblemSummary, Submission } from "../types/api";

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080";

export class ApiError extends Error {
  readonly status?: number;

  constructor(message: string, status?: number) {
    super(message);
    this.name = "ApiError";
    this.status = status;
  }
}

function describeErrorStatus(status: number): string {
  switch (status) {
    case 400:
      return "The server rejected the request (invalid input).";
    case 404:
      return "Not found.";
    default:
      return `Request failed with status ${status}.`;
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  let response: Response;
  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      headers: { "Content-Type": "application/json" },
      ...init,
    });
  } catch {
    throw new ApiError(
      `Could not reach the server at ${API_BASE_URL}. Is the backend running?`,
    );
  }

  if (!response.ok) {
    throw new ApiError(describeErrorStatus(response.status), response.status);
  }

  return (await response.json()) as T;
}

export function getProblems(): Promise<ProblemSummary[]> {
  return request<ProblemSummary[]>("/api/problems");
}

export function getProblem(id: number): Promise<ProblemDetail> {
  return request<ProblemDetail>(`/api/problems/${id}`);
}

export function submitSolution(problemId: number, sourceCode: string): Promise<Submission> {
  return request<Submission>(`/api/problems/${problemId}/submissions`, {
    method: "POST",
    body: JSON.stringify({ sourceCode }),
  });
}
