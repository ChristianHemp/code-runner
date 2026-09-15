package com.christianhemp.codejudge.execution;

/**
 * Outcome of a {@link CodeExecutor} run, shaped to map directly onto the
 * result fields of a {@code Submission} ({@code verdict}, {@code runtimeMs},
 * {@code errorMessage}).
 *
 * <p>{@code compiled} is explicit rather than inferred from a null
 * {@code verdict}: a submission that compiled but has not yet been judged
 * (no tests run) has {@code compiled=true, verdict=null}, which must never
 * be confused with {@code ACCEPTED}.
 */
public record ExecutionResult(boolean compiled, Verdict verdict, Long runtimeMs, String errorMessage) {
}
