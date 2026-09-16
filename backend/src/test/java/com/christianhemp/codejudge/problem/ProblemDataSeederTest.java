package com.christianhemp.codejudge.problem;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ProblemDataSeederTest {

	@Autowired
	private ProblemDataSeeder seeder;

	@Autowired
	private ProblemRepository problemRepository;

	@Autowired
	private TestCaseRepository testCaseRepository;

	@Test
	void reRunningTheSeederDoesNotDuplicateProblemsOrTestCases() {
		List<Problem> before = problemRepository.findAll();
		assertThat(before).extracting(Problem::getTitle).containsExactlyInAnyOrder(
				"Sum Two Integers", "Is Palindrome", "Maximum Element", "Count Vowels");

		int[] testCaseCountsBefore = before.stream().mapToInt(p -> testCaseRepository.findByProblemId(p.getId()).size()).toArray();

		seeder.run();

		List<Problem> after = problemRepository.findAll();
		assertThat(after).hasSameSizeAs(before);
		assertThat(after).extracting(Problem::getTitle).containsExactlyInAnyOrderElementsOf(
				before.stream().map(Problem::getTitle).toList());

		for (int i = 0; i < before.size(); i++) {
			Problem problem = before.get(i);
			assertThat(testCaseRepository.findByProblemId(problem.getId())).hasSize(testCaseCountsBefore[i]);
		}
	}

	@Test
	void everySeededProblemHasExecutionMetadata() {
		for (Problem problem : problemRepository.findAll()) {
			assertThat(problem.getMethodName()).as(problem.getTitle()).isNotNull();
			assertThat(problem.getSignatureShape()).as(problem.getTitle()).isNotNull();
		}
	}

}
