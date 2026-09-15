package com.christianhemp.codejudge.execution;

import java.time.Duration;

/**
 * Everything a {@link CodeExecutor} needs to run one Java submission.
 * Deliberately excludes the JPA {@code Submission} entity and any
 * persistence/HTTP concerns.
 */
public record ExecutionRequest(String sourceCode, Duration timeLimit) {
}
