package com.christianhemp.codejudge.submission;

import com.christianhemp.codejudge.problem.ProblemRepository;

import org.junit.jupiter.api.Test;
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

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class SubmissionApiIntegrationTest {

	private static final String SEEDED_TITLE = "Sum Two Integers";

	@Autowired
	private TestRestTemplate restTemplate;

	@Autowired
	private ProblemRepository problemRepository;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void createSubmissionForValidProblemReturnsPendingSubmission() throws Exception {
		Long problemId = problemRepository.findByTitle(SEEDED_TITLE).orElseThrow().getId();

		ResponseEntity<String> response = postSubmission(problemId, "public class Solution {}");

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		JsonNode body = objectMapper.readTree(response.getBody());

		assertThat(body.get("id").asLong()).isPositive();
		assertThat(body.get("problemId").asLong()).isEqualTo(problemId);
		assertThat(body.get("status").asString()).isEqualTo("PENDING");
		assertThat(body.get("verdict").isNull()).isTrue();
		assertThat(body.get("runtimeMs").isNull()).isTrue();
		assertThat(body.get("errorMessage").isNull()).isTrue();
		assertThat(body.get("completedAt").isNull()).isTrue();
		assertThat(body.get("submittedAt").asString()).isNotBlank();

		assertThat(body.propertyNames()).containsExactlyInAnyOrder(
				"id", "problemId", "status", "verdict", "runtimeMs", "errorMessage", "submittedAt", "completedAt");
		assertThat(response.getBody()).doesNotContain("sourceCode", "inputJson", "expectedOutputJson", "hidden");
	}

	@Test
	void createSubmissionForNonexistentProblemReturns404() {
		ResponseEntity<String> response = postSubmission(999999L, "public class Solution {}");

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
		ResponseEntity<String> created = postSubmission(problemId, "public class Solution {}");
		JsonNode createdBody = objectMapper.readTree(created.getBody());
		long submissionId = createdBody.get("id").asLong();

		ResponseEntity<String> response = restTemplate.getForEntity("/api/submissions/" + submissionId, String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		JsonNode body = objectMapper.readTree(response.getBody());
		assertThat(body.get("id").asLong()).isEqualTo(submissionId);
		assertThat(body.get("problemId").asLong()).isEqualTo(problemId);
		assertThat(body.get("status").asString()).isEqualTo("PENDING");
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
