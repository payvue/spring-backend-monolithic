package com.payvue.app.domain.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CompletionStrategyTest {

  @Test
  @DisplayName("RedirectCompletion은 성공 시 redirectUrl을 반환한다")
  void redirectCompletionUsesRedirectUrl() {
    CompletionStrategy strategy = new RedirectCompletion();
    AuthResult result = AuthResult.success(null).withMeta(RedirectRequired.REDIRECT_URL, "/next");

    AuthCompletion completion = strategy.complete(null, result);

    assertThat(completion).isInstanceOf(RedirectCompletionResult.class);
    assertThat(((RedirectCompletionResult) completion).redirectUrl()).isEqualTo("/next");
  }

  @Test
  @DisplayName("ConditionCompletionStrategy는 첫 매칭 규칙을 적용한다")
  void conditionCompletionStrategyAppliesFirstMatch() {
    CompletionStrategy strategy = new ConditionCompletionStrategy(
        CompletionStrategyClassification.OAUTH_DYNAMIC,
        List.of(
            new ConditionCompletionStrategy.CompletionRule(
                (ctx, res) -> true,
                new DirectCompletion()
            ),
            new ConditionCompletionStrategy.CompletionRule(
                (ctx, res) -> true,
                new RedirectCompletion()
            )
        )
    );

    AuthCompletion completion = strategy.complete(null, AuthResult.failure(AuthErrorCode.INVALID_REQUEST));

    assertThat(completion).isInstanceOf(DirectCompletionResult.class);
  }

  @Test
  @DisplayName("ConditionCompletionStrategy는 첫 매칭 규칙을 적용한다")
  void conditionCompletionStrategyAppliesSecondMatch() {
    CompletionStrategy strategy = new ConditionCompletionStrategy(
            CompletionStrategyClassification.OAUTH_DYNAMIC,
            List.of(
                    new ConditionCompletionStrategy.CompletionRule(
                            (ctx, res) -> false,
                            new DirectCompletion()
                    ),
                    new ConditionCompletionStrategy.CompletionRule(
                            (ctx, res) -> true,
                            new RedirectCompletion()
                    )
            )
    );

    AuthCompletion completion = strategy.complete(null, AuthResult.failure(AuthErrorCode.INVALID_REQUEST));

    assertThat(completion).isInstanceOf(RedirectCompletionResult.class);
  }

  @Test
  @DisplayName("NextStepCompletionStrategy는 다음 단계가 없으면 delegate를 사용한다")
  void nextStepCompletionUsesDelegateWhenNoNextStep() {
    CompletionStrategy strategy = new NextStepCompletionStrategy(new DirectCompletion(), null);

    AuthCompletion completion = strategy.complete(null, AuthResult.failure(AuthErrorCode.INVALID_REQUEST));

    assertThat(completion).isInstanceOf(DirectCompletionResult.class);
  }

  @Test
  @DisplayName("NextStepCompletionStrategy는 다음 단계가 있으면 해당 로직을 수행한다")
  void nextStepCompletionUsesNextStep() {
    NextStep nextStep = (result, context, delegate) ->
        new RedirectCompletionResult(result, "/next");
    CompletionStrategy strategy = new NextStepCompletionStrategy(new DirectCompletion(), nextStep);

    AuthCompletion completion = strategy.complete(null, AuthResult.failure(AuthErrorCode.INVALID_REQUEST));

    assertThat(completion).isInstanceOf(RedirectCompletionResult.class);
    assertThat(((RedirectCompletionResult) completion).redirectUrl()).isEqualTo("/next");
  }
}
