package com.christianhemp.codejudge.submission;

import jakarta.validation.constraints.NotBlank;

public record SubmissionCreateRequest(@NotBlank String sourceCode) {
}
