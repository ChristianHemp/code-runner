package com.christianhemp.codejudge.submission;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class SubmissionNotFoundException extends RuntimeException {

	public SubmissionNotFoundException(Long id) {
		super("Submission not found: " + id);
	}

}
