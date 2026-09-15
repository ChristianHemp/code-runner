package com.christianhemp.codejudge.problem;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/problems")
public class ProblemController {

	private final ProblemService problemService;

	public ProblemController(ProblemService problemService) {
		this.problemService = problemService;
	}

	@GetMapping
	public List<ProblemSummaryResponse> listProblems() {
		return problemService.listProblems();
	}

	@GetMapping("/{id}")
	public ProblemDetailResponse getProblem(@PathVariable Long id) {
		return problemService.getProblem(id);
	}

}
