package com.christianhemp.codejudge.execution;

import java.util.List;

/**
 * One hidden test case in execution-specific form: positional arguments the
 * generated harness passes to the problem's method, and the expected stdout
 * (compared after trimming surrounding whitespace).
 */
public record TestInput(List<String> arguments, String expectedOutput) {
}
