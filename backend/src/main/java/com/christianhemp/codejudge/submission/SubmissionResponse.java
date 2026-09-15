package com.christianhemp.codejudge.submission;

import java.time.Instant;

/**
 * Public view of a {@link Submission}. Deliberately omits source code - the
 * caller already has it, and it carries no problem test-case data.
 */
public record SubmissionResponse(
		Long id,
		Long problemId,
		SubmissionStatus status,
		Verdict verdict,
		Integer runtimeMs,
		String errorMessage,
		Instant submittedAt,
		Instant completedAt) {
}
