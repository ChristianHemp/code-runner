package com.christianhemp.codejudge.submission;

import com.christianhemp.codejudge.problem.ProblemRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class SubmissionApiIntegrationTest {

	private static final String SEEDED_TITLE = "Sum Two Integers";

	private static final String CORRECT_SOURCE =
			"public class Solution { public static int sum(int a, int b) { return a + b; } }";

	private static final String WRONG_SOURCE =
			"public class Solution { public static int sum(int a, int b) { return a - b; } }";

	@Autowired
	private TestRestTemplate restTemplate;

	@Autowired
	private ProblemRepository problemRepository;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void createSubmissionForCorrectSolutionReturnsCompletedAcceptedSubmission() throws Exception {
		Long problemId = problemRepository.findByTitle(SEEDED_TITLE).orElseThrow().getId();

		ResponseEntity<String> response = postSubmission(problemId, CORRECT_SOURCE);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		JsonNode body = objectMapper.readTree(response.getBody());

		assertThat(body.get("id").asLong()).isPositive();
		assertThat(body.get("problemId").asLong()).isEqualTo(problemId);
		assertThat(body.get("status").asString()).isEqualTo("COMPLETED");
		assertThat(body.get("verdict").asString()).isEqualTo("ACCEPTED");
		assertThat(body.get("runtimeMs").isNull()).isFalse();
		assertThat(body.get("errorMessage").isNull()).isTrue();
		assertThat(body.get("submittedAt").asString()).isNotBlank();
		assertThat(body.get("completedAt").asString()).isNotBlank();

		assertThat(body.propertyNames()).containsExactlyInAnyOrder(
				"id", "problemId", "status", "verdict", "runtimeMs", "errorMessage", "submittedAt", "completedAt");
		assertThat(response.getBody()).doesNotContain("sourceCode", "inputJson", "expectedOutputJson", "hidden");
	}

	@Test
	void createSubmissionForWrongSolutionReturnsCompletedWrongAnswerSubmission() throws Exception {
		Long problemId = problemRepository.findByTitle(SEEDED_TITLE).orElseThrow().getId();

		ResponseEntity<String> response = postSubmission(problemId, WRONG_SOURCE);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		JsonNode body = objectMapper.readTree(response.getBody());

		assertThat(body.get("status").asString()).isEqualTo("COMPLETED");
		assertThat(body.get("verdict").asString()).isEqualTo("WRONG_ANSWER");
		// The errorMessage must never reveal the hidden expected/actual values.
		assertThat(response.getBody()).doesNotContain("\"3\"", "\"-5\"", "\"-1\"");
	}

	@ParameterizedTest
	@MethodSource("otherProblems")
	void createSubmissionForOtherProblemsJudgesCorrectAndIncorrectSolutions(OtherProblemScenario scenario)
			throws Exception {
		Long problemId = problemRepository.findByTitle(scenario.title()).orElseThrow().getId();

		ResponseEntity<String> correctResponse = postSubmission(problemId, scenario.correctSource());
		JsonNode correctBody = objectMapper.readTree(correctResponse.getBody());
		assertThat(correctResponse.getStatusCode()).as(scenario.title() + " correct").isEqualTo(HttpStatus.CREATED);
		assertThat(correctBody.get("verdict").asString()).as(scenario.title() + " correct").isEqualTo("ACCEPTED");

		ResponseEntity<String> wrongResponse = postSubmission(problemId, scenario.wrongSource());
		JsonNode wrongBody = objectMapper.readTree(wrongResponse.getBody());
		assertThat(wrongBody.get("verdict").asString()).as(scenario.title() + " wrong").isEqualTo("WRONG_ANSWER");
	}

	private static Stream<OtherProblemScenario> otherProblems() {
		return Stream.of(
				new OtherProblemScenario(
						"Is Palindrome",
						"public class Solution { public static boolean isPalindrome(String s) { "
								+ "return new StringBuilder(s).reverse().toString().equals(s); } }",
						"public class Solution { public static boolean isPalindrome(String s) { return false; } }"),
				new OtherProblemScenario(
						"Maximum Element",
						"public class Solution { public static int maxElement(int[] nums) { "
								+ "int max = nums[0]; for (int n : nums) { if (n > max) max = n; } return max; } }",
						"public class Solution { public static int maxElement(int[] nums) { return 0; } }"),
				new OtherProblemScenario(
						"Count Vowels",
						"public class Solution { public static int countVowels(String s) { "
								+ "int c = 0; for (char ch : s.toCharArray()) { "
								+ "if (\"aeiouAEIOU\".indexOf(ch) >= 0) c++; } return c; } }",
						"public class Solution { public static int countVowels(String s) { return -1; } }"));
	}

	private record OtherProblemScenario(String title, String correctSource, String wrongSource) {
	}

	@Test
	void createSubmissionForNonexistentProblemReturns404() {
		ResponseEntity<String> response = postSubmission(999999L, CORRECT_SOURCE);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void createSubmissionWithBlankSourceCodeReturns400() {
		Long problemId = problemRepository.findByTitle(SEEDED_TITLE).orElseThrow().getId();

		ResponseEntity<String> response = postSubmission(problemId, "   ");

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
	}

	@Test
	void getSubmissionReturnsPersistedState() throws Exception {
		Long problemId = problemRepository.findByTitle(SEEDED_TITLE).orElseThrow().getId();
		ResponseEntity<String> created = postSubmission(problemId, CORRECT_SOURCE);
		JsonNode createdBody = objectMapper.readTree(created.getBody());
		long submissionId = createdBody.get("id").asLong();

		ResponseEntity<String> response = restTemplate.getForEntity("/api/submissions/" + submissionId, String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		JsonNode body = objectMapper.readTree(response.getBody());
		assertThat(body.get("id").asLong()).isEqualTo(submissionId);
		assertThat(body.get("problemId").asLong()).isEqualTo(problemId);
		assertThat(body.get("status").asString()).isEqualTo("COMPLETED");
		assertThat(body.get("verdict").asString()).isEqualTo("ACCEPTED");
	}

	@Test
	void getSubmissionReturns404ForNonexistentId() {
		ResponseEntity<String> response = restTemplate.getForEntity("/api/submissions/999999", String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	private ResponseEntity<String> postSubmission(Long problemId, String sourceCode) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		String requestBody = "{\"sourceCode\":" + objectMapper.writeValueAsString(sourceCode) + "}";
		return restTemplate.postForEntity(
				"/api/problems/" + problemId + "/submissions",
				new HttpEntity<>(requestBody, headers),
				String.class);
	}

}
