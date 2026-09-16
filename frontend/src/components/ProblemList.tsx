import type { ProblemSummary } from "../types/api";

interface ProblemListProps {
  problems: ProblemSummary[] | null;
  error: string | null;
  selectedId: number | null;
  onSelect: (id: number) => void;
}

export function ProblemList({ problems, error, selectedId, onSelect }: ProblemListProps) {
  return (
    <aside className="problem-list">
      <h2 className="panel-title">Problems</h2>
      {error && <p className="error-text">{error}</p>}
      {!error && problems === null && <p className="muted">Loading problems…</p>}
      {!error && problems !== null && problems.length === 0 && (
        <p className="muted">No problems available.</p>
      )}
      {!error && problems !== null && problems.length > 0 && (
        <ul>
          {problems.map((problem, index) => (
            <li key={problem.id}>
              <button
                type="button"
                className={problem.id === selectedId ? "problem-item selected" : "problem-item"}
                onClick={() => onSelect(problem.id)}
              >
                <span className="problem-index">{String(index + 1).padStart(2, "0")}</span>
                <span className="problem-title">{problem.title}</span>
              </button>
            </li>
          ))}
        </ul>
      )}
    </aside>
  );
}
