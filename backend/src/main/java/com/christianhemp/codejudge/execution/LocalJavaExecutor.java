package com.christianhemp.codejudge.execution;

import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Development {@link CodeExecutor}. Compiles submitted Java source with the
 * {@code javac} of whichever JDK is running this application, then - if
 * compilation succeeds - runs it against hidden tests with {@code java},
 * through a small generated harness.
 *
 * <p>Submitted source is always written as {@code Solution.java}, and the
 * generated harness is hardcoded to the current "Sum Two Integers" problem's
 * {@code public static int sum(int a, int b)} signature. Neither source
 * parsing nor a general per-problem harness generator is attempted here -
 * this milestone only needs to judge that one problem correctly.
 *
 * <p><strong>This is local, unsandboxed execution.</strong> Submitted code
 * now genuinely runs as an ordinary process on this host, with no resource
 * or filesystem isolation beyond its own temp directory. It must not be used
 * outside development until replaced by a properly isolated executor (e.g. a
 * future {@code DockerJavaExecutor}) - callers only ever depend on the
 * {@link CodeExecutor} interface, so that swap requires no changes here.
 */
@Component
public class LocalJavaExecutor implements CodeExecutor {

	private static final String SOURCE_FILE_NAME = "Solution.java";
	private static final String HARNESS_FILE_NAME = "JudgeHarness.java";
	private static final String HARNESS_CLASS_NAME = "JudgeHarness";
	private static final String HARNESS_SOURCE = """
			public class JudgeHarness {
			    public static void main(String[] args) {
			        int a = Integer.parseInt(args[0]);
			        int b = Integer.parseInt(args[1]);
			        System.out.print(Solution.sum(a, b));
			    }
			}
			""";

	private static final Duration DEFAULT_COMPILE_TIMEOUT = Duration.ofSeconds(10);
	private static final int MAX_CAPTURED_OUTPUT_BYTES = 64 * 1024;

	private final String javacPath;
	private final String javaPath;

	public LocalJavaExecutor() {
		this.javacPath = resolveJdkExecutable("javac");
		this.javaPath = resolveJdkExecutable("java");
	}

	@Override
	public ExecutionResult execute(ExecutionRequest request) {
		Path workingDirectory;
		try {
			workingDirectory = Files.createTempDirectory("code-judge-");
		} catch (IOException e) {
			throw new IllegalStateException("Could not create a temporary execution directory", e);
		}

		try {
			ExecutionResult compileResult = compile(request, workingDirectory);
			if (!compileResult.compiled()) {
				return compileResult;
			}
			return runHiddenTests(request, workingDirectory);
		} finally {
			deleteRecursively(workingDirectory);
		}
	}

	/**
	 * Timeout for the {@code javac} process. A protected hook (rather than a
	 * second constructor) so tests can force a near-zero timeout deterministically
	 * without adding an ambiguous constructor for Spring to resolve.
	 */
	protected Duration compileTimeout() {
		return DEFAULT_COMPILE_TIMEOUT;
	}

	private ExecutionResult compile(ExecutionRequest request, Path workingDirectory) {
		writeSource(workingDirectory.resolve(SOURCE_FILE_NAME), request.sourceCode());
		writeSource(workingDirectory.resolve(HARNESS_FILE_NAME), HARNESS_SOURCE);

		Process process = startProcess(
				List.of(javacPath, SOURCE_FILE_NAME, HARNESS_FILE_NAME), workingDirectory, true);

		OutputCollector output = new OutputCollector(process.getInputStream(), MAX_CAPTURED_OUTPUT_BYTES);
		Thread outputThread = startDaemonThread(output, "javac-output-reader");

		if (!waitFor(process, compileTimeout())) {
			process.destroyForcibly();
			waitUninterruptibly(process);
			joinQuietly(outputThread);
			return new ExecutionResult(false, Verdict.COMPILATION_ERROR, null,
					"Compilation timed out after " + compileTimeout().toSeconds() + "s");
		}

		joinQuietly(outputThread);

		if (process.exitValue() != 0) {
			return new ExecutionResult(false, Verdict.COMPILATION_ERROR, null, output.capturedOutput());
		}

		return new ExecutionResult(true, null, null, null);
	}

	/**
	 * Runs each hidden test in order against the compiled {@code Solution},
	 * stopping at the first non-{@code ACCEPTED} outcome per the documented
	 * verdict precedence. {@code runtimeMs} covers only this test-running phase,
	 * not compilation.
	 */
	private ExecutionResult runHiddenTests(ExecutionRequest request, Path workingDirectory) {
		long startNanos = System.nanoTime();

		for (TestInput testInput : request.testInputs()) {
			RunOutcome outcome = runOneTest(testInput, workingDirectory, request.timeLimit());
			if (outcome.verdict() != Verdict.ACCEPTED) {
				return new ExecutionResult(true, outcome.verdict(), elapsedMs(startNanos), outcome.errorMessage());
			}
		}

		return new ExecutionResult(true, Verdict.ACCEPTED, elapsedMs(startNanos), null);
	}

	private RunOutcome runOneTest(TestInput testInput, Path workingDirectory, Duration timeLimit) {
		List<String> command = new ArrayList<>(List.of(javaPath, "-cp", ".", HARNESS_CLASS_NAME));
		command.addAll(testInput.arguments());

		Process process = startProcess(command, workingDirectory, false);

		OutputCollector stdout = new OutputCollector(process.getInputStream(), MAX_CAPTURED_OUTPUT_BYTES);
		OutputCollector stderr = new OutputCollector(process.getErrorStream(), MAX_CAPTURED_OUTPUT_BYTES);
		Thread stdoutThread = startDaemonThread(stdout, "harness-stdout-reader");
		Thread stderrThread = startDaemonThread(stderr, "harness-stderr-reader");

		if (!waitFor(process, timeLimit)) {
			process.destroyForcibly();
			waitUninterruptibly(process);
			joinQuietly(stdoutThread);
			joinQuietly(stderrThread);
			return new RunOutcome(Verdict.TIME_LIMIT_EXCEEDED,
					"Execution exceeded the time limit (" + timeLimit.toMillis() + "ms).");
		}

		joinQuietly(stdoutThread);
		joinQuietly(stderrThread);

		if (process.exitValue() != 0) {
			String diagnostic = !stderr.capturedOutput().isBlank() ? stderr.capturedOutput() : stdout.capturedOutput();
			return new RunOutcome(Verdict.RUNTIME_ERROR, diagnostic);
		}

		String actual = stdout.capturedOutput().trim();
		String expected = testInput.expectedOutput().trim();
		if (actual.equals(expected)) {
			return new RunOutcome(Verdict.ACCEPTED, null);
		}

		// Deliberately generic: the expected/actual values belong to a hidden test
		// case and must never reach the public SubmissionResponse.errorMessage.
		return new RunOutcome(Verdict.WRONG_ANSWER, "Output did not match the expected result.");
	}

	private static long elapsedMs(long startNanos) {
		return (System.nanoTime() - startNanos) / 1_000_000;
	}

	private Process startProcess(List<String> command, Path workingDirectory, boolean mergeErrorStream) {
		try {
			return new ProcessBuilder(command)
					.directory(workingDirectory.toFile())
					.redirectErrorStream(mergeErrorStream)
					.start();
		} catch (IOException e) {
			throw new IllegalStateException("Could not start process: " + command, e);
		}
	}

	private static boolean waitFor(Process process, Duration timeout) {
		try {
			return process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			process.destroyForcibly();
			throw new IllegalStateException("Interrupted while waiting for process", e);
		}
	}

	private static void writeSource(Path file, String content) {
		try {
			Files.writeString(file, content, StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new IllegalStateException("Could not write source to disk: " + file, e);
		}
	}

	private static Thread startDaemonThread(Runnable runnable, String name) {
		Thread thread = new Thread(runnable, name);
		thread.setDaemon(true);
		thread.start();
		return thread;
	}

	/**
	 * Resolves an executable from the running JVM's own installation rather than
	 * a hardcoded path, so this works on any machine/CI runner as long as the
	 * application itself is running on a full JDK (which building it already
	 * requires).
	 */
	private static String resolveJdkExecutable(String name) {
		String javaHome = System.getProperty("java.home");
		boolean windows = System.getProperty("os.name", "").toLowerCase().contains("win");
		Path executable = Path.of(javaHome, "bin", windows ? name + ".exe" : name);
		if (!Files.isExecutable(executable)) {
			throw new IllegalStateException(
					"No " + name + " executable found at " + executable
							+ " - the running JVM must be a full JDK, not a JRE.");
		}
		return executable.toString();
	}

	private static void waitUninterruptibly(Process process) {
		boolean interrupted = false;
		while (true) {
			try {
				process.waitFor();
				break;
			} catch (InterruptedException e) {
				interrupted = true;
			}
		}
		if (interrupted) {
			Thread.currentThread().interrupt();
		}
	}

	private static void joinQuietly(Thread thread) {
		try {
			thread.join(1000);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	private static void deleteRecursively(Path root) {
		try (var paths = Files.walk(root)) {
			paths.sorted(Comparator.reverseOrder()).forEach(path -> {
				try {
					Files.delete(path);
				} catch (IOException ignored) {
					// best-effort cleanup
				}
			});
		} catch (IOException ignored) {
			// best-effort cleanup
		}
	}

	private record RunOutcome(Verdict verdict, String errorMessage) {
	}

	/**
	 * Drains a process's output stream on a background thread so the process can
	 * never block on a full pipe, capping retained bytes at {@code maxBytes} so
	 * pathological output cannot exhaust memory.
	 */
	private static final class OutputCollector implements Runnable {

		private final InputStream inputStream;
		private final int maxBytes;
		private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

		OutputCollector(InputStream inputStream, int maxBytes) {
			this.inputStream = inputStream;
			this.maxBytes = maxBytes;
		}

		@Override
		public void run() {
			byte[] chunk = new byte[4096];
			try {
				int read;
				while ((read = inputStream.read(chunk)) != -1) {
					int remaining = maxBytes - buffer.size();
					if (remaining > 0) {
						buffer.write(chunk, 0, Math.min(read, remaining));
					}
				}
			} catch (IOException ignored) {
				// process was destroyed; nothing more to capture
			}
		}

		String capturedOutput() {
			return buffer.toString(StandardCharsets.UTF_8);
		}

	}

}
