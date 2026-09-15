package com.christianhemp.codejudge.problem;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "problems")
public class Problem {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
	private String title;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String description;

	@Column(nullable = false)
	private String methodSignature;

	@OneToMany(mappedBy = "problem", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	private List<TestCase> testCases = new ArrayList<>();

	protected Problem() {
	}

	public Problem(String title, String description, String methodSignature) {
		this.title = title;
		this.description = description;
		this.methodSignature = methodSignature;
	}

	public void addTestCase(TestCase testCase) {
		testCases.add(testCase);
		testCase.assignToProblem(this);
	}

	public Long getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}

	public String getDescription() {
		return description;
	}

	public String getMethodSignature() {
		return methodSignature;
	}

	public List<TestCase> getTestCases() {
		return testCases;
	}

}
