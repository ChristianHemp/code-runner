package com.christianhemp.codejudge.execution;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class LocalJavaExecutorTest {

	private static final String VALID_SOURCE = """
			public class Solution {
			    public static int sum(int a, int b) {
			        return a + b;
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

	private static final String SOURCE_WITH_MAIN = """
			public class Solution {
			    public static void main(String[] args) {
			        throw new RuntimeException("SHOULD_NOT_RUN");
			    }
			}
			""";

	@Test
	void validSourceCompilesSuccessfully() {
		LocalJavaExecutor executor = new LocalJavaExecutor();

		ExecutionResult result = executor.execute(new ExecutionRequest(VALID_SOURCE, Duration.ofSeconds(10)));

		assertThat(result.compiled()).isTrue();
		assertThat(result.errorMessage()).isNull();
	}

	@Test
	void successfulCompilationDoesNotClaimAccepted() {
		LocalJavaExecutor executor = new LocalJavaExecutor();

		ExecutionResult result = executor.execute(new ExecutionRequest(VALID_SOURCE, Duration.ofSeconds(10)));

		assertThat(result.verdict()).isNull();
		assertThat(result.verdict()).isNotEqualTo(Verdict.ACCEPTED);
	}

	@Test
	void invalidSourceReturnsCompilationError() {
		LocalJavaExecutor executor = new LocalJavaExecutor();

		ExecutionResult result = executor.execute(new ExecutionRequest(INVALID_SOURCE, Duration.ofSeconds(10)));

		assertThat(result.compiled()).isFalse();
		assertThat(result.verdict()).isEqualTo(Verdict.COMPILATION_ERROR);
	}

	@Test
	void compilerDiagnosticsAreCaptured() {
		LocalJavaExecutor executor = new LocalJavaExecutor();

		ExecutionResult result = executor.execute(new ExecutionRequest(INVALID_SOURCE, Duration.ofSeconds(10)));

		assertThat(result.errorMessage()).isNotBlank();
		assertThat(result.errorMessage().toLowerCase()).contains("error");
	}

	@Test
	void temporaryDirectoryIsCleanedUpAfterSuccessfulCompilation() {
		LocalJavaExecutor executor = new LocalJavaExecutor();
		long before = countCodeJudgeTempDirs();

		executor.execute(new ExecutionRequest(VALID_SOURCE, Duration.ofSeconds(10)));

		assertThat(countCodeJudgeTempDirs()).isEqualTo(before);
	}

	@Test
	void temporaryDirectoryIsCleanedUpAfterFailedCompilation() {
		LocalJavaExecutor executor = new LocalJavaExecutor();
		long before = countCodeJudgeTempDirs();

		executor.execute(new ExecutionRequest(INVALID_SOURCE, Duration.ofSeconds(10)));

		assertThat(countCodeJudgeTempDirs()).isEqualTo(before);
	}

	@Test
	@Timeout(value = 5)
	void compilerTimeoutIsHandledWithoutLeavingALiveProcess() {
		LocalJavaExecutor executor = new LocalJavaExecutor() {
			@Override
			protected Duration compileTimeout() {
				return Duration.ofNanos(1);
			}
		};
		long before = countCodeJudgeTempDirs();

		ExecutionResult result = executor.execute(new ExecutionRequest(VALID_SOURCE, Duration.ofSeconds(10)));

		assertThat(result.compiled()).isFalse();
		assertThat(result.verdict()).isEqualTo(Verdict.COMPILATION_ERROR);
		assertThat(result.errorMessage()).contains("timed out");
		assertThat(countCodeJudgeTempDirs()).isEqualTo(before);
	}

	@Test
	void submittedCodeIsNotExecuted() {
		LocalJavaExecutor executor = new LocalJavaExecutor();

		ExecutionResult result = executor.execute(new ExecutionRequest(SOURCE_WITH_MAIN, Duration.ofSeconds(10)));

		assertThat(result.compiled()).isTrue();
		assertThat(result.errorMessage()).isNull();
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
