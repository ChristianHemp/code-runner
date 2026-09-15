package com.christianhemp.codejudge.submission;

import com.christianhemp.codejudge.problem.Problem;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "submissions")
public class Submission {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "problem_id", nullable = false)
	private Problem problem;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String sourceCode;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private SubmissionStatus status;

	@Enumerated(EnumType.STRING)
	private Verdict verdict;

	private Integer runtimeMs;

	@Column(columnDefinition = "TEXT")
	private String errorMessage;

	@Column(nullable = false)
	private Instant submittedAt;

	private Instant completedAt;

	protected Submission() {
	}

	public Submission(Problem problem, String sourceCode) {
		this.problem = problem;
		this.sourceCode = sourceCode;
		this.status = SubmissionStatus.PENDING;
		this.submittedAt = Instant.now();
	}

	public Long getId() {
		return id;
	}

	public Problem getProblem() {
		return problem;
	}

	public String getSourceCode() {
		return sourceCode;
	}

	public SubmissionStatus getStatus() {
		return status;
	}

	public Verdict getVerdict() {
		return verdict;
	}

	public Integer getRuntimeMs() {
		return runtimeMs;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public Instant getSubmittedAt() {
		return submittedAt;
	}

	public Instant getCompletedAt() {
		return completedAt;
	}

}
