package com.christianhemp.codejudge.problem;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class ProblemDataSeeder implements CommandLineRunner {

	private static final String SUM_TWO_INTEGERS_TITLE = "Sum Two Integers";

	private final ProblemRepository problemRepository;

	public ProblemDataSeeder(ProblemRepository problemRepository) {
		this.problemRepository = problemRepository;
	}

	@Override
	public void run(String... args) {
		if (problemRepository.findByTitle(SUM_TWO_INTEGERS_TITLE).isPresent()) {
			return;
		}

		Problem problem = new Problem(
				SUM_TWO_INTEGERS_TITLE,
				"Implement a method that returns the sum of two integers.",
				"public static int sum(int a, int b)");

		problem.addTestCase(new TestCase("[1, 2]", "3", true));
		problem.addTestCase(new TestCase("[-2, -3]", "-5", true));
		problem.addTestCase(new TestCase("[0, 0]", "0", true));
		problem.addTestCase(new TestCase("[-5, 8]", "3", true));

		problemRepository.save(problem);
	}

}
