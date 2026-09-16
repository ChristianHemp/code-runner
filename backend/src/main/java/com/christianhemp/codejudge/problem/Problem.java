package com.christianhemp.codejudge.problem;

import com.christianhemp.codejudge.execution.SignatureShape;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

	/**
	 * Name of the method the harness calls on {@code Solution} (e.g.
	 * {@code "sum"}). Kept separate from {@link #methodSignature}, which is the
	 * human-readable display string - the executor must never need to parse
	 * that string.
	 *
	 * <p>Deliberately not {@code nullable = false} at the column level: adding a
	 * NOT NULL column via {@code ddl-auto=update} to a table that may already
	 * have rows is unsafe without a migration tool. Non-null is enforced by the
	 * constructor and backfilled for pre-existing rows by {@link ProblemDataSeeder}.
	 */
	@Column
	private String methodName;

	/** @see #methodName */
	@Enumerated(EnumType.STRING)
	@Column
	private SignatureShape signatureShape;

	@OneToMany(mappedBy = "problem", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	private List<TestCase> testCases = new ArrayList<>();

	protected Problem() {
	}

	public Problem(
			String title,
			String description,
			String methodSignature,
			String methodName,
			SignatureShape signatureShape) {
		this.title = title;
		this.description = description;
		this.methodSignature = methodSignature;
		this.methodName = methodName;
		this.signatureShape = signatureShape;
	}

	public void addTestCase(TestCase testCase) {
		testCases.add(testCase);
		testCase.assignToProblem(this);
	}

	/** Backfills execution metadata on a problem seeded before it existed. */
	public void updateExecutionMetadata(String methodName, SignatureShape signatureShape) {
		this.methodName = methodName;
		this.signatureShape = signatureShape;
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

	public String getMethodName() {
		return methodName;
	}

	public SignatureShape getSignatureShape() {
		return signatureShape;
	}

	public List<TestCase> getTestCases() {
		return testCases;
	}

}
