package com.payvue.app.domain.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.payvue.supporting.user.UserContextManager;
import com.payvue.supporting.user.command.User;
import com.payvue.supporting.user.command.UserAccountView;
import com.payvue.supporting.user.command.UserId;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OAuthAuthVerifierTest {

  @Test
  @DisplayName("이메일이 없으면 INVALID_REQUEST를 반환한다")
  void returnsInvalidRequestWhenEmailMissing() {
    OAuthAuthVerifier verifier = new OAuthAuthVerifier(new StubUserContextManager(Optional.empty()));

    AuthResult result = verifier.handle(new AuthRequest(null, "secret", null), null);

    assertThat(result.error().code()).isEqualTo(AuthErrorCode.INVALID_REQUEST);
  }

  @Test
  @DisplayName("사용자가 존재하면 성공 결과를 반환한다")
  void returnsSuccessWhenUserExists() {
    User user = newUser();
    OAuthAuthVerifier verifier = new OAuthAuthVerifier(new StubUserContextManager(Optional.of(user)));

    AuthResult result = verifier.handle(AuthRequest.ofOAuth("google", "id", "a@b.com"), null);

    assertThat(result.success()).isTrue();
    assertThat(result.user().userId()).isEqualTo(user.getId());
  }

  @Test
  @DisplayName("사용자가 없으면 USER_NOT_FOUND와 redirect meta를 반환한다")
  void returnsRedirectWhenUserMissing() {
    OAuthAuthVerifier verifier = new OAuthAuthVerifier(new StubUserContextManager(Optional.empty()));

    AuthResult result = verifier.handle(AuthRequest.ofOAuth("google", "id", "a@b.com"), null);

    assertThat(result.error().code()).isEqualTo(AuthErrorCode.USER_NOT_FOUND);
    assertThat(result.meta()).containsKey(RedirectRequired.REDIRECT_URL);
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
        "12345"
    );
  }

  static final class StubUserContextManager implements UserContextManager {
    private final Optional<User> user;

    StubUserContextManager(Optional<User> user) {
      this.user = user;
    }

    @Override
    public void save(User user, com.payvue.supporting.user.UserCommand command) {}

    @Override
    public <T> Optional<T> findById(UserId id, Class<T> viewType) {
      return Optional.empty();
    }

    @Override
    public <T> Optional<T> findByEmail(String email, Class<T> viewType) {
      if (viewType.equals(UserAccountView.class)) {
        return user.map(UserAccountView::from).map(viewType::cast);
      }
      return Optional.empty();
    }
  }
}
