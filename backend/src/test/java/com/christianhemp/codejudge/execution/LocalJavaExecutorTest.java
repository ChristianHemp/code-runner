package com.christianhemp.codejudge.execution;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class LocalJavaExecutorTest {

	private static final String CORRECT_SOURCE = """
			public class Solution {
			    public static int sum(int a, int b) {
			        return a + b;
			    }
			}
			""";

	private static final String INCORRECT_SOURCE = """
			public class Solution {
			    public static int sum(int a, int b) {
			        return a - b;
			    }
			}
			""";

	private static final String INVALID_SOURCE = """
			public class Solution {
			    public static int sum(int a, int b) {
			        return a +
			    }
			}
			""";

	private static final String THROWING_SOURCE = """
			public class Solution {
			    public static int sum(int a, int b) {
			        throw new RuntimeException("boom");
			    }
			}
			""";

	private static final String INFINITE_LOOP_SOURCE = """
			public class Solution {
			    public static int sum(int a, int b) {
			        while (true) {}
			    }
			}
			""";

	private static final String SOURCE_WITH_UNUSED_MAIN = """
			public class Solution {
			    public static int sum(int a, int b) {
			        return a + b;
			    }
			    public static void main(String[] args) {
			        throw new RuntimeException("SHOULD_NOT_RUN");
			    }
			}
			""";

	private static final Duration DEFAULT_TIME_LIMIT = Duration.ofSeconds(2);

	@Test
	void correctSolutionIsAccepted() {
		ExecutionResult result = new LocalJavaExecutor().execute(
				new ExecutionRequest(CORRECT_SOURCE, DEFAULT_TIME_LIMIT, sumTestInputs()));

		assertThat(result.compiled()).isTrue();
		assertThat(result.verdict()).isEqualTo(Verdict.ACCEPTED);
		assertThat(result.errorMessage()).isNull();
		assertThat(result.runtimeMs()).isNotNull();
	}

	@Test
	void incorrectSolutionIsWrongAnswer() {
		ExecutionResult result = new LocalJavaExecutor().execute(
				new ExecutionRequest(INCORRECT_SOURCE, DEFAULT_TIME_LIMIT, sumTestInputs()));

		assertThat(result.compiled()).isTrue();
		assertThat(result.verdict()).isEqualTo(Verdict.WRONG_ANSWER);
	}

	@Test
	void wrongAnswerMessageDoesNotLeakHiddenTestValues() {
		ExecutionResult result = new LocalJavaExecutor().execute(
				new ExecutionRequest(INCORRECT_SOURCE, DEFAULT_TIME_LIMIT, sumTestInputs()));

		assertThat(result.errorMessage()).doesNotContain("3", "-5", "-1");
	}

	@Test
	void syntaxErrorIsCompilationError() {
		ExecutionResult result = new LocalJavaExecutor().execute(
				new ExecutionRequest(INVALID_SOURCE, DEFAULT_TIME_LIMIT, sumTestInputs()));

		assertThat(result.compiled()).isFalse();
		assertThat(result.verdict()).isEqualTo(Verdict.COMPILATION_ERROR);
	}

	@Test
	void compilerDiagnosticsAreCaptured() {
		ExecutionResult result = new LocalJavaExecutor().execute(
				new ExecutionRequest(INVALID_SOURCE, DEFAULT_TIME_LIMIT, sumTestInputs()));

		assertThat(result.errorMessage()).isNotBlank();
		assertThat(result.errorMessage().toLowerCase()).contains("error");
	}

	@Test
	void runtimeExceptionIsRuntimeError() {
		ExecutionResult result = new LocalJavaExecutor().execute(
				new ExecutionRequest(THROWING_SOURCE, DEFAULT_TIME_LIMIT, sumTestInputs()));

		assertThat(result.compiled()).isTrue();
		assertThat(result.verdict()).isEqualTo(Verdict.RUNTIME_ERROR);
		assertThat(result.errorMessage()).isNotBlank();
	}

	@Test
	@Timeout(5)
	void infiniteLoopIsTimeLimitExceeded() {
		ExecutionResult result = new LocalJavaExecutor().execute(
				new ExecutionRequest(INFINITE_LOOP_SOURCE, Duration.ofMillis(300), sumTestInputs()));

		assertThat(result.compiled()).isTrue();
		assertThat(result.verdict()).isEqualTo(Verdict.TIME_LIMIT_EXCEEDED);
		assertThat(result.errorMessage()).contains("time limit");
	}

	@Test
	@Timeout(5)
	void executionStopsAtTheFirstFailingTest() {
		// First hidden test uses a=1 and gets a deliberately wrong answer; every
		// later test would hang forever if reached. A quick WRONG_ANSWER (not a
		// slow TIME_LIMIT_EXCEEDED) proves later tests were never attempted.
		String source = """
				public class Solution {
				    public static int sum(int a, int b) {
				        if (a == 1) {
				            return 999;
				        }
				        while (true) {}
				    }
				}
				""";

		ExecutionResult result = new LocalJavaExecutor().execute(
				new ExecutionRequest(source, Duration.ofMillis(300), sumTestInputs()));

		assertThat(result.verdict()).isEqualTo(Verdict.WRONG_ANSWER);
	}

	@Test
	void submittedMainMethodIsNeverExecuted() {
		ExecutionResult result = new LocalJavaExecutor().execute(
				new ExecutionRequest(SOURCE_WITH_UNUSED_MAIN, DEFAULT_TIME_LIMIT, sumTestInputs()));

		// If the harness ever invoked main() instead of sum(), this would surface
		// as a RUNTIME_ERROR (the thrown exception), not ACCEPTED.
		assertThat(result.verdict()).isEqualTo(Verdict.ACCEPTED);
	}

	@Test
	void temporaryDirectoryIsCleanedUpForEveryVerdict() {
		record Scenario(String source, Duration timeLimit) {
		}

		List<Scenario> scenarios = List.of(
				new Scenario(CORRECT_SOURCE, DEFAULT_TIME_LIMIT),
				new Scenario(INCORRECT_SOURCE, DEFAULT_TIME_LIMIT),
				new Scenario(INVALID_SOURCE, DEFAULT_TIME_LIMIT),
				new Scenario(THROWING_SOURCE, DEFAULT_TIME_LIMIT),
				new Scenario(INFINITE_LOOP_SOURCE, Duration.ofMillis(300)));

		LocalJavaExecutor executor = new LocalJavaExecutor();
		for (Scenario scenario : scenarios) {
			long before = countCodeJudgeTempDirs();
			executor.execute(new ExecutionRequest(scenario.source(), scenario.timeLimit(), sumTestInputs()));
			assertThat(countCodeJudgeTempDirs()).as("scenario: %s", scenario.source()).isEqualTo(before);
		}
	}

	@Test
	@Timeout(value = 5)
	void compileTimeoutIsHandledWithoutLeavingALiveProcess() {
		LocalJavaExecutor executor = new LocalJavaExecutor() {
			@Override
			protected Duration compileTimeout() {
				return Duration.ofNanos(1);
			}
		};
		long before = countCodeJudgeTempDirs();

		ExecutionResult result = executor.execute(
				new ExecutionRequest(CORRECT_SOURCE, DEFAULT_TIME_LIMIT, sumTestInputs()));

		assertThat(result.compiled()).isFalse();
		assertThat(result.verdict()).isEqualTo(Verdict.COMPILATION_ERROR);
		assertThat(result.errorMessage()).contains("timed out");
		assertThat(countCodeJudgeTempDirs()).isEqualTo(before);
	}

	private static List<TestInput> sumTestInputs() {
		return List.of(
				new TestInput(List.of("1", "2"), "3"),
				new TestInput(List.of("-2", "-3"), "-5"),
				new TestInput(List.of("0", "0"), "0"),
				new TestInput(List.of("-5", "8"), "3"));
	}

	private static long countCodeJudgeTempDirs() {
		Path tempRoot = Path.of(System.getProperty("java.io.tmpdir"));
		try (Stream<Path> entries = Files.list(tempRoot)) {
			return entries.filter(path -> path.getFileName().toString().startsWith("code-judge-")).count();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

}
