package com.christianhemp.codejudge.execution;

/**
 * Boundary between the submission domain and however Java code is actually
 * compiled and run. Implementations may run locally, in a container, or
 * otherwise - callers only depend on this interface.
 */
public interface CodeExecutor {

	ExecutionResult execute(ExecutionRequest request);

}
