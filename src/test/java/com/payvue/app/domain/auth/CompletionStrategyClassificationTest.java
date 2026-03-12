package com.payvue.app.domain.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CompletionStrategyClassificationTest {

  @Test
  @DisplayName("DIRECT 전략은 VERIFY만 지원한다")
  void directSupportsVerifyOnly() {
    assertThat(CompletionStrategyClassification.DIRECT.supports(
        CompletionStrategyClassification.Sequence.VERIFIFY)).isTrue();
    assertThat(CompletionStrategyClassification.DIRECT.supports(
        CompletionStrategyClassification.Sequence.ISSUE)).isFalse();
  }

  @Test
  @DisplayName("REDIRECT 전략은 ISSUE와 VERIFY를 지원한다")
  void redirectSupportsIssueAndVerify() {
    assertThat(CompletionStrategyClassification.REDIRECT.supports(
        CompletionStrategyClassification.Sequence.VERIFIFY)).isTrue();
    assertThat(CompletionStrategyClassification.REDIRECT.supports(
        CompletionStrategyClassification.Sequence.ISSUE)).isTrue();
  }

  @Test
  @DisplayName("failure는 전략 타입에 맞는 결과를 반환한다")
  void failureReturnsExpectedResultType() {
    AuthCompletion direct = CompletionStrategyClassification.failure(
        CompletionStrategyClassification.DIRECT,
        AuthErrorCode.INVALID_REQUEST
    );
    AuthCompletion redirect = CompletionStrategyClassification.failure(
        CompletionStrategyClassification.REDIRECT,
        AuthErrorCode.INVALID_REQUEST
    );

    assertThat(direct).isInstanceOf(DirectCompletionResult.class);
    assertThat(redirect).isInstanceOf(RedirectCompletionResult.class);
  }
}
