package com.christianhemp.codejudge.problem;

/**
 * Public listing view of a {@link Problem}. Deliberately has no field capable
 * of carrying {@link TestCase} data.
 */
public record ProblemSummaryResponse(Long id, String title) {
}
