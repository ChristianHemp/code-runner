package com.christianhemp.codejudge.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Allows the local Vite dev server to call the API during development.
 * Deliberately a fixed, explicit origin - not a wildcard - since this is not
 * a production CORS policy.
 */
@Configuration
public class WebConfig {

	private static final String LOCAL_FRONTEND_ORIGIN = "http://localhost:5173";

	@Bean
	public WebMvcConfigurer corsConfigurer() {
		return new WebMvcConfigurer() {
			@Override
			public void addCorsMappings(CorsRegistry registry) {
				registry.addMapping("/api/**")
						.allowedOrigins(LOCAL_FRONTEND_ORIGIN)
						.allowedMethods("GET", "POST")
						.allowedHeaders("Content-Type");
			}
		};
	}

}
