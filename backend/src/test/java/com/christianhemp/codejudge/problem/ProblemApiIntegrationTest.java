package com.christianhemp.codejudge.problem;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class ProblemApiIntegrationTest {

	private static final String SEEDED_TITLE = "Sum Two Integers";

	@Autowired
	private TestRestTemplate restTemplate;

	@Autowired
	private ProblemRepository problemRepository;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void listProblemsReturnsSeededProblem() throws Exception {
		ResponseEntity<String> response = restTemplate.getForEntity("/api/problems", String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		JsonNode body = objectMapper.readTree(response.getBody());
		assertThat(body.isArray()).isTrue();

		JsonNode seeded = findByTitle(body, SEEDED_TITLE);
		assertThat(seeded).isNotNull();
		assertThat(seeded.propertyNames()).containsExactlyInAnyOrder("id", "title");
	}

	@Test
	void getProblemReturnsPublicDetailsWithoutHiddenData() throws Exception {
		Long id = problemRepository.findByTitle(SEEDED_TITLE).orElseThrow().getId();

		ResponseEntity<String> response = restTemplate.getForEntity("/api/problems/" + id, String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		JsonNode body = objectMapper.readTree(response.getBody());

		assertThat(body.get("id").asLong()).isEqualTo(id);
		assertThat(body.get("title").asString()).isEqualTo(SEEDED_TITLE);
		assertThat(body.get("description").asString())
				.isEqualTo("Implement a method that returns the sum of two integers.");
		assertThat(body.get("methodSignature").asString()).isEqualTo("public static int sum(int a, int b)");

		assertThat(body.propertyNames()).containsExactlyInAnyOrder("id", "title", "description", "methodSignature");
		assertThat(response.getBody()).doesNotContain("inputJson", "expectedOutputJson", "testCases", "hidden");
	}

	@Test
	void getProblemReturns404ForNonexistentId() {
		ResponseEntity<String> response = restTemplate.getForEntity("/api/problems/999999", String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	private JsonNode findByTitle(JsonNode array, String title) {
		for (JsonNode node : array) {
			if (title.equals(node.path("title").asString())) {
				return node;
			}
		}
		return null;
	}

}
