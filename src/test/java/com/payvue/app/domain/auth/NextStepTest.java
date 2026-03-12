package com.payvue.app.domain.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.payvue.supporting.user.command.User;
import com.payvue.supporting.user.command.UserAccountView;
import com.payvue.supporting.user.command.UserPasswordGuard;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;

class NextStepTest {

  @Test
  @DisplayName("AuthenticateComplete는 delegate를 그대로 수행한다")
  void authenticateCompleteDelegates() {
    NextStep nextStep = new AuthenticateComplete();
    AuthResult result = AuthResult.failure(AuthErrorCode.INVALID_REQUEST);

    AuthCompletion completion = nextStep.apply(result, null, new DirectCompletion());

    assertThat(completion).isInstanceOf(DirectCompletionResult.class);
    assertThat(completion.result()).isEqualTo(result);
  }

  @Test
  @DisplayName("RedirectRequired는 redirectUrl 메타가 없으면 예외가 발생한다")
  void redirectRequiredThrowsWhenMissingMeta() {
    NextStep nextStep = new RedirectRequired();
    AuthResult result = AuthResult.failure(AuthErrorCode.INVALID_REQUEST);

    assertThatThrownBy(() -> nextStep.apply(result, null, new DirectCompletion()))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  @DisplayName("RedirectRequired는 redirectUrl 메타가 있으면 delegate를 수행한다")
  void redirectRequiredDelegatesWhenMetaPresent() {
    NextStep nextStep = new RedirectRequired();
    AuthResult result = AuthResult.success(UserAccountView.from(newUser()))
        .withMeta(RedirectRequired.REDIRECT_URL, "/next");

    AuthCompletion completion = nextStep.apply(result, null, new RedirectCompletion());

    assertThat(completion).isInstanceOf(RedirectCompletionResult.class);
    assertThat(((RedirectCompletionResult) completion).redirectUrl()).isEqualTo("/next");
  }

  private User newUser() {
    return User.create(
        "login",
        "a@b.com",
        "010",
        "nick",
        "real",
        LocalDate.now(),
        "sido",
        "sigungu",
        "road",
        "detail",
        "12345",
        "encoded",
        UserPasswordGuard.defaultPolicy("Passw0rd!")
    );
  }
}
