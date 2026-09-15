package com.christianhemp.codejudge.problem;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Judge-only fixture data. Never referenced by public DTOs or controllers -
 * {@link #inputJson} and {@link #expectedOutputJson} must stay reachable only
 * through {@link ProblemService} and {@link TestCaseRepository}.
 */
@Entity
@Table(name = "test_cases")
public class TestCase {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "problem_id", nullable = false)
	private Problem problem;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String inputJson;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String expectedOutputJson;

	@Column(nullable = false)
	private boolean hidden = true;

	protected TestCase() {
	}

	public TestCase(String inputJson, String expectedOutputJson, boolean hidden) {
		this.inputJson = inputJson;
		this.expectedOutputJson = expectedOutputJson;
		this.hidden = hidden;
	}

	void assignToProblem(Problem problem) {
		this.problem = problem;
	}

	public Long getId() {
		return id;
	}

	public Problem getProblem() {
		return problem;
	}

	public String getInputJson() {
		return inputJson;
	}

	public String getExpectedOutputJson() {
		return expectedOutputJson;
	}

	public boolean isHidden() {
		return hidden;
	}

}
