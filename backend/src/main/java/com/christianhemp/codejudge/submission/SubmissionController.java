package com.christianhemp.codejudge.submission;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SubmissionController {

	private final SubmissionService submissionService;

	public SubmissionController(SubmissionService submissionService) {
		this.submissionService = submissionService;
	}

	@PostMapping("/api/problems/{problemId}/submissions")
	@ResponseStatus(HttpStatus.CREATED)
	public SubmissionResponse createSubmission(
			@PathVariable Long problemId,
			@Valid @RequestBody SubmissionCreateRequest request) {
		return submissionService.createSubmission(problemId, request);
	}

	@GetMapping("/api/submissions/{submissionId}")
	public SubmissionResponse getSubmission(@PathVariable Long submissionId) {
		return submissionService.getSubmission(submissionId);
	}

}
