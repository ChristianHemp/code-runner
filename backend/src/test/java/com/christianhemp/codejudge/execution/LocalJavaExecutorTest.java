package com.christianhemp.codejudge.execution;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalJavaExecutorTest {

	@Test
	void satisfiesTheCodeExecutorContract() {
		CodeExecutor executor = new LocalJavaExecutor();

		assertThat(executor).isInstanceOf(CodeExecutor.class);
	}

	@Test
	void executeDoesNotYetSupportRunningCode() {
		CodeExecutor executor = new LocalJavaExecutor();
		ExecutionRequest request = new ExecutionRequest("public class Solution {}", Duration.ofSeconds(5));

		assertThatThrownBy(() -> executor.execute(request))
				.isInstanceOf(UnsupportedOperationException.class);
	}

}
