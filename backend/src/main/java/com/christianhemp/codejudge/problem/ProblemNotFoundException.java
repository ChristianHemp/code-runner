package com.christianhemp.codejudge.problem;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ProblemNotFoundException extends RuntimeException {

	public ProblemNotFoundException(Long id) {
		super("Problem not found: " + id);
	}

}
