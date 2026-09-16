package com.christianhemp.codejudge.execution;

/**
 * Identifies which {@code Solution} method the harness should call and how
 * to convert hidden-test arguments into that method's parameters.
 */
public record MethodSignature(String methodName, SignatureShape shape) {
}
