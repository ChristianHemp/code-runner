package com.christianhemp.codejudge.execution;

import java.time.Duration;
import java.util.List;

/**
 * Everything a {@link CodeExecutor} needs to compile and judge one Java
 * submission. Deliberately excludes the JPA {@code Submission}/{@code
 * TestCase} entities and any persistence/HTTP concerns - {@code testInputs}
 * carries only the plain argument/expected-output values the executor needs.
 *
 * <p>{@code timeLimit} bounds each hidden test run individually. Compilation
 * has its own separate, non-configurable timeout inside the executor.
 */
public record ExecutionRequest(String sourceCode, Duration timeLimit, List<TestInput> testInputs) {
}
