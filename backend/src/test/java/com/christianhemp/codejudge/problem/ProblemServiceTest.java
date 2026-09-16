package com.christianhemp.codejudge.problem;

import com.christianhemp.codejudge.execution.SignatureShape;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProblemServiceTest {

	@Mock
	private ProblemRepository problemRepository;

	@Test
	void listProblemsReturnsSummariesForAllProblems() {
		Problem problem = new Problem(
				"Sum Two Integers", "Add two ints.", "public static int sum(int a, int b)",
				"sum", SignatureShape.INT_INT_TO_INT);
		when(problemRepository.findAll()).thenReturn(List.of(problem));

		ProblemService service = new ProblemService(problemRepository);
		List<ProblemSummaryResponse> summaries = service.listProblems();

		assertThat(summaries).hasSize(1);
		assertThat(summaries.get(0).title()).isEqualTo("Sum Two Integers");
	}

	@Test
	void getProblemReturnsDetailForExistingId() {
		Problem problem = new Problem(
				"Sum Two Integers", "Add two ints.", "public static int sum(int a, int b)",
				"sum", SignatureShape.INT_INT_TO_INT);
		when(problemRepository.findById(1L)).thenReturn(Optional.of(problem));

		ProblemService service = new ProblemService(problemRepository);
		ProblemDetailResponse detail = service.getProblem(1L);

		assertThat(detail.title()).isEqualTo("Sum Two Integers");
		assertThat(detail.description()).isEqualTo("Add two ints.");
		assertThat(detail.methodSignature()).isEqualTo("public static int sum(int a, int b)");
	}

	@Test
	void getProblemThrowsNotFoundForMissingId() {
		when(problemRepository.findById(99L)).thenReturn(Optional.empty());

		ProblemService service = new ProblemService(problemRepository);

		assertThatThrownBy(() -> service.getProblem(99L))
				.isInstanceOf(ProblemNotFoundException.class);
	}

}
