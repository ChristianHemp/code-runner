package com.christianhemp.codejudge.execution;

import org.springframework.stereotype.Component;

/**
 * Development {@link CodeExecutor}. Execution itself arrives in Milestone 5
 * (temp directories, {@code javac}, {@code ProcessBuilder}); this milestone
 * only establishes the Spring wiring and implementation boundary.
 */
@Component
public class LocalJavaExecutor implements CodeExecutor {

	@Override
	public ExecutionResult execute(ExecutionRequest request) {
		throw new UnsupportedOperationException(
				"LocalJavaExecutor does not yet support code execution - this arrives in Milestone 5.");
	}

}
