package com.christianhemp.codejudge.submission;

import com.christianhemp.codejudge.execution.CodeExecutor;
import com.christianhemp.codejudge.execution.ExecutionRequest;
import com.christianhemp.codejudge.execution.ExecutionResult;
import com.christianhemp.codejudge.execution.MethodSignature;
import com.christianhemp.codejudge.execution.TestInput;
import com.christianhemp.codejudge.execution.Verdict;
import com.christianhemp.codejudge.problem.Problem;
import com.christianhemp.codejudge.problem.ProblemNotFoundException;
import com.christianhemp.codejudge.problem.ProblemRepository;
import com.christianhemp.codejudge.problem.TestCase;
import com.christianhemp.codejudge.problem.TestCaseRepository;

import org.springframework.stereotype.Service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
public class SubmissionService {

	private static final Duration TEST_TIME_LIMIT = Duration.ofSeconds(2);

	private final SubmissionRepository submissionRepository;
	private final ProblemRepository problemRepository;
	private final TestCaseRepository testCaseRepository;
	private final CodeExecutor codeExecutor;
	private final ObjectMapper objectMapper;

	public SubmissionService(
			SubmissionRepository submissionRepository,
			ProblemRepository problemRepository,
			TestCaseRepository testCaseRepository,
			CodeExecutor codeExecutor,
			ObjectMapper objectMapper) {
		this.submissionRepository = submissionRepository;
		this.problemRepository = problemRepository;
		this.testCaseRepository = testCaseRepository;
		this.codeExecutor = codeExecutor;
		this.objectMapper = objectMapper;
	}

	/**
	 * Judging runs synchronously within this call - there is no queue yet, so the
	 * HTTP request that creates a submission blocks until a final verdict is
	 * persisted. Asynchronous execution is a later milestone.
	 */
	public SubmissionResponse createSubmission(Long problemId, SubmissionCreateRequest request) {
		Problem problem = problemRepository.findById(problemId)
				.orElseThrow(() -> new ProblemNotFoundException(problemId));

		Submission submission = new Submission(problem, request.sourceCode());
		submissionRepository.save(submission);

		judge(submission, problem);

		return toResponse(submission);
	}

	public SubmissionResponse getSubmission(Long submissionId) {
		Submission submission = submissionRepository.findById(submissionId)
				.orElseThrow(() -> new SubmissionNotFoundException(submissionId));

		return toResponse(submission);
	}

	private void judge(Submission submission, Problem problem) {
		submission.markRunning();
		submissionRepository.save(submission);

		try {
			List<TestInput> testInputs = loadHiddenTests(problem.getId());
			MethodSignature methodSignature = new MethodSignature(problem.getMethodName(), problem.getSignatureShape());
			ExecutionRequest executionRequest =
					new ExecutionRequest(submission.getSourceCode(), TEST_TIME_LIMIT, methodSignature, testInputs);
			ExecutionResult result = codeExecutor.execute(executionRequest);

			Integer runtimeMs = result.runtimeMs() == null ? null : result.runtimeMs().intValue();
			submission.complete(result.verdict(), runtimeMs, result.errorMessage());
		} catch (RuntimeException e) {
			// An unexpected executor/infrastructure failure must not leave the
			// submission stuck in RUNNING forever; RUNTIME_ERROR is the closest fit
			// among the fixed verdicts for "this submission could not be judged."
			submission.complete(Verdict.RUNTIME_ERROR, null, "Judging failed unexpectedly.");
		}

		submissionRepository.save(submission);
	}

	private List<TestInput> loadHiddenTests(Long problemId) {
		List<TestInput> testInputs = new ArrayList<>();
		for (TestCase testCase : testCaseRepository.findByProblemId(problemId)) {
			testInputs.add(toTestInput(testCase));
		}
		return testInputs;
	}

	private TestInput toTestInput(TestCase testCase) {
		List<String> arguments = new ArrayList<>();
		JsonNode argumentsNode = objectMapper.readTree(testCase.getInputJson());
		for (JsonNode argument : argumentsNode) {
			arguments.add(argument.asText());
		}
		return new TestInput(arguments, testCase.getExpectedOutputJson());
	}

	private SubmissionResponse toResponse(Submission submission) {
		return new SubmissionResponse(
				submission.getId(),
				submission.getProblem().getId(),
				submission.getStatus(),
				submission.getVerdict(),
				submission.getRuntimeMs(),
				submission.getErrorMessage(),
				submission.getSubmittedAt(),
				submission.getCompletedAt());
	}

}
