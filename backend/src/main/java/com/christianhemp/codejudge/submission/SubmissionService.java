package com.christianhemp.codejudge.submission;

import com.christianhemp.codejudge.problem.Problem;
import com.christianhemp.codejudge.problem.ProblemNotFoundException;
import com.christianhemp.codejudge.problem.ProblemRepository;

import org.springframework.stereotype.Service;

@Service
public class SubmissionService {

	private final SubmissionRepository submissionRepository;
	private final ProblemRepository problemRepository;

	public SubmissionService(SubmissionRepository submissionRepository, ProblemRepository problemRepository) {
		this.submissionRepository = submissionRepository;
		this.problemRepository = problemRepository;
	}

	public SubmissionResponse createSubmission(Long problemId, SubmissionCreateRequest request) {
		Problem problem = problemRepository.findById(problemId)
				.orElseThrow(() -> new ProblemNotFoundException(problemId));

		Submission submission = new Submission(problem, request.sourceCode());
		submissionRepository.save(submission);

		return toResponse(submission);
	}

	public SubmissionResponse getSubmission(Long submissionId) {
		Submission submission = submissionRepository.findById(submissionId)
				.orElseThrow(() -> new SubmissionNotFoundException(submissionId));

		return toResponse(submission);
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
