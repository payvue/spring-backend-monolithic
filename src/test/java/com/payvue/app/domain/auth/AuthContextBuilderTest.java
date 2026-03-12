package com.payvue.app.domain.auth;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AuthContextBuilderTest {

  @Test
  @DisplayName("컨텍스트 없이 context 호출 시 예외가 발생한다")
  void contextRequiresUnit() {
    assertThatThrownBy(() -> AuthContextBuilder.builder()
        .context(spec -> spec
            .strategy(new DirectCompletion())
        )
        .build())
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  @DisplayName("completionStrategy가 없으면 예외가 발생한다")
  void buildRequiresCompletionStrategy() {
    assertThatThrownBy(() -> AuthContextBuilder.builder()
        .unit(Authentication.NONE.name(), spec -> {})
        .build())
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  @DisplayName("전략 타입이 일치하지 않으면 예외가 발생한다")
  void strategyTypeMismatchThrows() {
    assertThatThrownBy(() -> AuthContextBuilder.builder()
        .unit(Authentication.NONE.name(), CompletionStrategyClassification.DIRECT.name(),
            spec -> spec.strategy(CompletionStrategyClassification.DIRECT.name(), () -> new RedirectCompletion()))
        .build())
        .isInstanceOf(IllegalArgumentException.class);
  }
}
