package com.christianhemp.codejudge.problem;

import com.christianhemp.codejudge.execution.SignatureShape;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Seeds the demo problem set. Idempotent by title: a problem already present
 * is left alone except for backfilling {@code methodName}/{@code
 * signatureShape} if a row was created before those columns existed (see
 * {@link Problem#updateExecutionMetadata}) - existing test cases are never
 * duplicated or touched.
 */
@Component
public class ProblemDataSeeder implements CommandLineRunner {

	private final ProblemRepository problemRepository;

	public ProblemDataSeeder(ProblemRepository problemRepository) {
		this.problemRepository = problemRepository;
	}

	@Override
	public void run(String... args) {
		for (ProblemDefinition definition : PROBLEMS) {
			Optional<Problem> existing = problemRepository.findByTitle(definition.title());
			if (existing.isPresent()) {
				Problem problem = existing.get();
				problem.updateExecutionMetadata(definition.methodName(), definition.signatureShape());
				problemRepository.save(problem);
				continue;
			}
			problemRepository.save(definition.toProblem());
		}
	}

	private record ProblemDefinition(
			String title,
			String description,
			String methodSignature,
			String methodName,
			SignatureShape signatureShape,
			List<TestCase> testCases) {

		Problem toProblem() {
			Problem problem = new Problem(title, description, methodSignature, methodName, signatureShape);
			testCases.forEach(problem::addTestCase);
			return problem;
		}
	}

	private static final List<ProblemDefinition> PROBLEMS = List.of(
			new ProblemDefinition(
					"Sum Two Integers",
					"Implement a method that returns the sum of two integers.",
					"public static int sum(int a, int b)",
					"sum",
					SignatureShape.INT_INT_TO_INT,
					List.of(
							new TestCase("[1, 2]", "3", true),
							new TestCase("[-2, -3]", "-5", true),
							new TestCase("[0, 0]", "0", true),
							new TestCase("[-5, 8]", "3", true))),
			new ProblemDefinition(
					"Is Palindrome",
					"Implement a method that returns true if the given string reads the same "
							+ "forward and backward. Comparison is case-sensitive.",
					"public static boolean isPalindrome(String s)",
					"isPalindrome",
					SignatureShape.STRING_TO_BOOLEAN,
					List.of(
							new TestCase("[\"racecar\"]", "true", true),
							new TestCase("[\"hello\"]", "false", true),
							new TestCase("[\"a\"]", "true", true),
							new TestCase("[\"\"]", "true", true),
							new TestCase("[\"Aba\"]", "false", true))),
			new ProblemDefinition(
					"Maximum Element",
					"Implement a method that returns the largest value in a non-empty array of integers.",
					"public static int maxElement(int[] nums)",
					"maxElement",
					SignatureShape.INT_ARRAY_TO_INT,
					List.of(
							new TestCase("[\"1,5,3\"]", "5", true),
							new TestCase("[\"-10,-3,-7\"]", "-3", true),
							new TestCase("[\"42\"]", "42", true),
							new TestCase("[\"-1,0,1\"]", "1", true))),
			new ProblemDefinition(
					"Count Vowels",
					"Implement a method that returns the number of vowels (a, e, i, o, u, "
							+ "case-insensitive) in the given string.",
					"public static int countVowels(String s)",
					"countVowels",
					SignatureShape.STRING_TO_INT,
					List.of(
							new TestCase("[\"hello\"]", "2", true),
							new TestCase("[\"AEIOU\"]", "5", true),
							new TestCase("[\"xyz\"]", "0", true),
							new TestCase("[\"Hello World\"]", "3", true),
							new TestCase("[\"\"]", "0", true))));

}
