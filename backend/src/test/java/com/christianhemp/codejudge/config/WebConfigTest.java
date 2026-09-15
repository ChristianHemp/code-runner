package com.christianhemp.codejudge.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class WebConfigTest {

	@Autowired
	private TestRestTemplate restTemplate;

	@Test
	void allowsPreflightRequestsFromTheLocalViteDevServer() {
		ResponseEntity<Void> response = preflightFrom("http://localhost:5173");

		assertThat(response.getHeaders().getAccessControlAllowOrigin()).isEqualTo("http://localhost:5173");
	}

	@Test
	void doesNotAllowPreflightRequestsFromOtherOrigins() {
		ResponseEntity<Void> response = preflightFrom("http://evil.example.com");

		assertThat(response.getHeaders().getAccessControlAllowOrigin()).isNull();
	}

	private ResponseEntity<Void> preflightFrom(String origin) {
		HttpHeaders headers = new HttpHeaders();
		headers.set("Origin", origin);
		headers.set("Access-Control-Request-Method", "GET");
		return restTemplate.exchange("/api/problems", HttpMethod.OPTIONS, new HttpEntity<>(headers), Void.class);
	}

}
