package com.christianhemp.codejudge.problem;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProblemService {

	private final ProblemRepository problemRepository;

	public ProblemService(ProblemRepository problemRepository) {
		this.problemRepository = problemRepository;
	}

	public List<ProblemSummaryResponse> listProblems() {
		return problemRepository.findAll().stream()
				.map(problem -> new ProblemSummaryResponse(problem.getId(), problem.getTitle()))
				.toList();
	}

	public ProblemDetailResponse getProblem(Long id) {
		Problem problem = problemRepository.findById(id)
				.orElseThrow(() -> new ProblemNotFoundException(id));
		return new ProblemDetailResponse(
				problem.getId(),
				problem.getTitle(),
				problem.getDescription(),
				problem.getMethodSignature());
	}

}
