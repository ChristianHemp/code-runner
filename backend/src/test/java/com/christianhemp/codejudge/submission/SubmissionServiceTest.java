package com.christianhemp.codejudge.submission;

import com.christianhemp.codejudge.problem.Problem;
import com.christianhemp.codejudge.problem.ProblemNotFoundException;
import com.christianhemp.codejudge.problem.ProblemRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubmissionServiceTest {

	@Mock
	private SubmissionRepository submissionRepository;

	@Mock
	private ProblemRepository problemRepository;

	@Test
	void createSubmissionPersistsSubmissionInPendingStateForExistingProblem() throws Exception {
		Problem problem = new Problem("Sum Two Integers", "Add two ints.", "public static int sum(int a, int b)");
		setId(problem, 1L);
		when(problemRepository.findById(1L)).thenReturn(Optional.of(problem));

		SubmissionService service = new SubmissionService(submissionRepository, problemRepository);
		SubmissionCreateRequest request = new SubmissionCreateRequest("public class Solution {}");

		SubmissionResponse response = service.createSubmission(1L, request);

		ArgumentCaptor<Submission> captor = ArgumentCaptor.forClass(Submission.class);
		verify(submissionRepository).save(captor.capture());
		Submission saved = captor.getValue();

		assertThat(saved.getProblem()).isEqualTo(problem);
		assertThat(saved.getSourceCode()).isEqualTo("public class Solution {}");
		assertThat(saved.getStatus()).isEqualTo(SubmissionStatus.PENDING);
		assertThat(saved.getVerdict()).isNull();
		assertThat(saved.getRuntimeMs()).isNull();
		assertThat(saved.getErrorMessage()).isNull();
		assertThat(saved.getCompletedAt()).isNull();
		assertThat(saved.getSubmittedAt()).isNotNull();

		assertThat(response.problemId()).isEqualTo(1L);
		assertThat(response.status()).isEqualTo(SubmissionStatus.PENDING);
		assertThat(response.verdict()).isNull();
	}

	@Test
	void createSubmissionThrowsNotFoundForMissingProblem() {
		when(problemRepository.findById(99L)).thenReturn(Optional.empty());

		SubmissionService service = new SubmissionService(submissionRepository, problemRepository);
		SubmissionCreateRequest request = new SubmissionCreateRequest("public class Solution {}");

		assertThatThrownBy(() -> service.createSubmission(99L, request))
				.isInstanceOf(ProblemNotFoundException.class);
	}

	@Test
	void getSubmissionReturnsPersistedStateForExistingId() throws Exception {
		Problem problem = new Problem("Sum Two Integers", "Add two ints.", "public static int sum(int a, int b)");
		setId(problem, 1L);
		Submission submission = new Submission(problem, "public class Solution {}");
		setId(submission, 5L);
		when(submissionRepository.findById(5L)).thenReturn(Optional.of(submission));

		SubmissionService service = new SubmissionService(submissionRepository, problemRepository);
		SubmissionResponse response = service.getSubmission(5L);

		assertThat(response.id()).isEqualTo(5L);
		assertThat(response.problemId()).isEqualTo(1L);
		assertThat(response.status()).isEqualTo(SubmissionStatus.PENDING);
	}

	@Test
	void getSubmissionThrowsNotFoundForMissingId() {
		when(submissionRepository.findById(404L)).thenReturn(Optional.empty());

		SubmissionService service = new SubmissionService(submissionRepository, problemRepository);

		assertThatThrownBy(() -> service.getSubmission(404L))
				.isInstanceOf(SubmissionNotFoundException.class);
	}

	private static void setId(Object entity, Long id) throws Exception {
		Field field = entity.getClass().getDeclaredField("id");
		field.setAccessible(true);
		field.set(entity, id);
	}

}
