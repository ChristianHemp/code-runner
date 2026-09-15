package com.christianhemp.codejudge.problem;

/**
 * Public detail view of a {@link Problem}. Deliberately has no field capable
 * of carrying {@link TestCase} data.
 */
public record ProblemDetailResponse(Long id, String title, String description, String methodSignature) {
}
