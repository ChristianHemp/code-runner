package com.christianhemp.codejudge.execution;

import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;

/**
 * Development {@link CodeExecutor}. Compiles submitted Java source with the
 * {@code javac} of whichever JDK is running this application - it does not
 * yet run the compiled program (Milestone 6).
 *
 * <p>Submitted source is always written as {@code Solution.java}; this
 * milestone does not parse source to discover the public class name.
 */
@Component
public class LocalJavaExecutor implements CodeExecutor {

	private static final String SOURCE_FILE_NAME = "Solution.java";
	private static final Duration DEFAULT_COMPILE_TIMEOUT = Duration.ofSeconds(10);
	private static final int MAX_CAPTURED_OUTPUT_BYTES = 64 * 1024;

	private final String javacPath;

	public LocalJavaExecutor() {
		this.javacPath = resolveJavac();
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
			return compile(request, workingDirectory);
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
		Path sourceFile = workingDirectory.resolve(SOURCE_FILE_NAME);
		try {
			Files.writeString(sourceFile, request.sourceCode(), StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new IllegalStateException("Could not write submitted source to disk", e);
		}

		Process process;
		try {
			process = new ProcessBuilder(javacPath, SOURCE_FILE_NAME)
					.directory(workingDirectory.toFile())
					.redirectErrorStream(true)
					.start();
		} catch (IOException e) {
			throw new IllegalStateException("Could not start the Java compiler process", e);
		}

		OutputCollector outputCollector = new OutputCollector(process.getInputStream(), MAX_CAPTURED_OUTPUT_BYTES);
		Thread outputThread = new Thread(outputCollector, "javac-output-reader");
		outputThread.setDaemon(true);
		outputThread.start();

		boolean finishedInTime;
		try {
			finishedInTime = process.waitFor(compileTimeout().toMillis(), TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			process.destroyForcibly();
			throw new IllegalStateException("Interrupted while waiting for compilation", e);
		}

		if (!finishedInTime) {
			process.destroyForcibly();
			waitUninterruptibly(process);
			joinQuietly(outputThread);
			return new ExecutionResult(false, Verdict.COMPILATION_ERROR, null,
					"Compilation timed out after " + compileTimeout().toSeconds() + "s");
		}

		joinQuietly(outputThread);

		if (process.exitValue() != 0) {
			return new ExecutionResult(false, Verdict.COMPILATION_ERROR, null, outputCollector.capturedOutput());
		}

		return new ExecutionResult(true, null, null, null);
	}

	/**
	 * Resolves {@code javac} from the running JVM's own installation rather than
	 * a hardcoded path, so this works on any machine/CI runner as long as the
	 * application itself is running on a full JDK (which building it already
	 * requires).
	 */
	private static String resolveJavac() {
		String javaHome = System.getProperty("java.home");
		boolean windows = System.getProperty("os.name", "").toLowerCase().contains("win");
		Path javac = Path.of(javaHome, "bin", windows ? "javac.exe" : "javac");
		if (!Files.isExecutable(javac)) {
			throw new IllegalStateException(
					"No javac executable found at " + javac + " - the running JVM must be a full JDK, not a JRE.");
		}
		return javac.toString();
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

	/**
	 * Drains a process's combined output stream on a background thread so the
	 * process can never block on a full pipe, capping retained bytes at
	 * {@code maxBytes} so pathological compiler output cannot exhaust memory.
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
