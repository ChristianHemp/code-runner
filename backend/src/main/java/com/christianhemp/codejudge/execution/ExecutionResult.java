package com.christianhemp.codejudge.execution;

/**
 * Outcome of a {@link CodeExecutor} run, shaped to map directly onto the
 * result fields of a {@code Submission} ({@code verdict}, {@code runtimeMs},
 * {@code errorMessage}).
 */
public record ExecutionResult(Verdict verdict, Long runtimeMs, String errorMessage) {
}
