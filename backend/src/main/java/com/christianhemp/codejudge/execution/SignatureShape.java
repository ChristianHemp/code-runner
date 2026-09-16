package com.christianhemp.codejudge.execution;

/**
 * The closed set of {@code Solution} method shapes {@link LocalJavaExecutor}
 * knows how to generate a harness invocation for. Deliberately a small fixed
 * enum rather than a general parameter/return-type model - adding a new
 * shape means adding one case to the harness generator, not building a type
 * system.
 */
public enum SignatureShape {
	INT_INT_TO_INT,
	STRING_TO_BOOLEAN,
	STRING_TO_INT,
	INT_ARRAY_TO_INT
}
