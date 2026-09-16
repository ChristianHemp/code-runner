package com.christianhemp.codejudge.submission;

import com.christianhemp.codejudge.execution.CodeExecutor;
import com.christianhemp.codejudge.execution.ExecutionRequest;
import com.christianhemp.codejudge.execution.ExecutionResult;
import com.christianhemp.codejudge.execution.SignatureShape;
import com.christianhemp.codejudge.execution.Verdict;
import com.christianhemp.codejudge.problem.Problem;
import com.christianhemp.codejudge.problem.ProblemNotFoundException;
import com.christianhemp.codejudge.problem.ProblemRepository;
import com.christianhemp.codejudge.problem.TestCase;
import com.christianhemp.codejudge.problem.TestCaseRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import tools.jackson.databind.ObjectMapper;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubmissionServiceTest {

	@Mock
	private SubmissionRepository submissionRepository;

	@Mock
	private ProblemRepository problemRepository;

	@Mock
	private TestCaseRepository testCaseRepository;

	@Mock
	private CodeExecutor codeExecutor;

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	void createSubmissionRunsFullLifecycleAndPersistsFinalVerdict() throws Exception {
		Problem problem = new Problem(
				"Sum Two Integers", "Add two ints.", "public static int sum(int a, int b)",
				"sum", SignatureShape.INT_INT_TO_INT);
		setId(problem, 1L);
		when(problemRepository.findById(1L)).thenReturn(Optional.of(problem));
		when(testCaseRepository.findByProblemId(1L)).thenReturn(List.of(new TestCase("[1, 2]", "3", true)));

		List<SubmissionStatus> statusesAtEachSave = new ArrayList<>();
		when(submissionRepository.save(any(Submission.class))).thenAnswer(invocation -> {
			Submission submission = invocation.getArgument(0);
			statusesAtEachSave.add(submission.getStatus());
			return submission;
		});

		when(codeExecutor.execute(any(ExecutionRequest.class)))
				.thenReturn(new ExecutionResult(true, Verdict.ACCEPTED, 42L, null));

		SubmissionService service = new SubmissionService(
				submissionRepository, problemRepository, testCaseRepository, codeExecutor, objectMapper);
		SubmissionCreateRequest request = new SubmissionCreateRequest("public class Solution {}");

		SubmissionResponse response = service.createSubmission(1L, request);

		assertThat(statusesAtEachSave)
				.containsExactly(SubmissionStatus.PENDING, SubmissionStatus.RUNNING, SubmissionStatus.COMPLETED);

		assertThat(response.status()).isEqualTo(SubmissionStatus.COMPLETED);
		assertThat(response.verdict()).isEqualTo(Verdict.ACCEPTED);
		assertThat(response.runtimeMs()).isEqualTo(42);
		assertThat(response.errorMessage()).isNull();

		ArgumentCaptor<ExecutionRequest> executionRequestCaptor = ArgumentCaptor.forClass(ExecutionRequest.class);
		verify(codeExecutor).execute(executionRequestCaptor.capture());
		ExecutionRequest executionRequest = executionRequestCaptor.getValue();
		assertThat(executionRequest.sourceCode()).isEqualTo("public class Solution {}");
		assertThat(executionRequest.methodSignature().methodName()).isEqualTo("sum");
		assertThat(executionRequest.methodSignature().shape()).isEqualTo(SignatureShape.INT_INT_TO_INT);
		assertThat(executionRequest.testInputs()).hasSize(1);
		assertThat(executionRequest.testInputs().get(0).arguments()).containsExactly("1", "2");
		assertThat(executionRequest.testInputs().get(0).expectedOutput()).isEqualTo("3");
	}

	@Test
	void unexpectedExecutorFailureCompletesSubmissionAsRuntimeErrorInsteadOfStayingRunning() throws Exception {
		Problem problem = new Problem(
				"Sum Two Integers", "Add two ints.", "public static int sum(int a, int b)",
				"sum", SignatureShape.INT_INT_TO_INT);
		setId(problem, 1L);
		when(problemRepository.findById(1L)).thenReturn(Optional.of(problem));
		when(testCaseRepository.findByProblemId(1L)).thenReturn(List.of());
		when(submissionRepository.save(any(Submission.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(codeExecutor.execute(any(ExecutionRequest.class))).thenThrow(new IllegalStateException("boom"));

		SubmissionService service = new SubmissionService(
				submissionRepository, problemRepository, testCaseRepository, codeExecutor, objectMapper);
		SubmissionCreateRequest request = new SubmissionCreateRequest("public class Solution {}");

		SubmissionResponse response = service.createSubmission(1L, request);

		assertThat(response.status()).isEqualTo(SubmissionStatus.COMPLETED);
		assertThat(response.verdict()).isEqualTo(Verdict.RUNTIME_ERROR);
	}

	@Test
	void createSubmissionThrowsNotFoundForMissingProblem() {
		when(problemRepository.findById(99L)).thenReturn(Optional.empty());

		SubmissionService service = new SubmissionService(
				submissionRepository, problemRepository, testCaseRepository, codeExecutor, objectMapper);
		SubmissionCreateRequest request = new SubmissionCreateRequest("public class Solution {}");

		assertThatThrownBy(() -> service.createSubmission(99L, request))
				.isInstanceOf(ProblemNotFoundException.class);
	}

	@Test
	void getSubmissionReturnsPersistedStateForExistingId() throws Exception {
		Problem problem = new Problem(
				"Sum Two Integers", "Add two ints.", "public static int sum(int a, int b)",
				"sum", SignatureShape.INT_INT_TO_INT);
		setId(problem, 1L);
		Submission submission = new Submission(problem, "public class Solution {}");
		setId(submission, 5L);
		when(submissionRepository.findById(5L)).thenReturn(Optional.of(submission));

		SubmissionService service = new SubmissionService(
				submissionRepository, problemRepository, testCaseRepository, codeExecutor, objectMapper);
		SubmissionResponse response = service.getSubmission(5L);

		assertThat(response.id()).isEqualTo(5L);
		assertThat(response.problemId()).isEqualTo(1L);
		assertThat(response.status()).isEqualTo(SubmissionStatus.PENDING);
	}

	@Test
	void getSubmissionThrowsNotFoundForMissingId() {
		when(submissionRepository.findById(404L)).thenReturn(Optional.empty());

		SubmissionService service = new SubmissionService(
				submissionRepository, problemRepository, testCaseRepository, codeExecutor, objectMapper);

		assertThatThrownBy(() -> service.getSubmission(404L))
				.isInstanceOf(SubmissionNotFoundException.class);
	}

	private static void setId(Object entity, Long id) throws Exception {
		Field field = entity.getClass().getDeclaredField("id");
		field.setAccessible(true);
		field.set(entity, id);
	}

}
