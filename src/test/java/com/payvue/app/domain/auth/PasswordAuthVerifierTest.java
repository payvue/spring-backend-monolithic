package com.payvue.app.domain.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.payvue.app.domain.security.PasswordHash;
import com.payvue.supporting.user.UserContextManager;
import com.payvue.supporting.user.command.User;
import com.payvue.supporting.user.command.UserPasswordGuard;
import com.payvue.supporting.user.command.UserAccountView;
import com.payvue.supporting.user.command.UserId;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PasswordAuthVerifierTest {

  @Test
  @DisplayName("사용자가 없으면 예외가 발생한다")
  void throwsWhenUserMissing() {
    UserContextManager userContextManager = new StubUserContextManager(Optional.empty());
    PasswordAuthVerifier verifier = new PasswordAuthVerifier(new AuthService(new MatchingPasswordHasher()));
    AuthContext context = new AuthContext(
        Authentication.PASSWORD,
        CompletionStrategyClassification.DIRECT,
        userContextManager,
        new AuthOrchestratorTest.StubSessionManager(),
        new DirectCompletion(),
        java.util.Map.of()
    );

    assertThatThrownBy(() -> verifier.handle(AuthRequest.ofPassword("a@b.com", "Passw0rd!"), context))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("비밀번호가 일치하지 않으면 INVALID_PASSWORD를 반환한다")
  void returnsInvalidPasswordWhenMismatch() {
    User user = newUser();
    UserContextManager userContextManager = new StubUserContextManager(Optional.of(user));
    PasswordAuthVerifier verifier = new PasswordAuthVerifier(new AuthService(new RejectingPasswordHasher()));
    AuthContext context = new AuthContext(
        Authentication.PASSWORD,
        CompletionStrategyClassification.DIRECT,
        userContextManager,
        new AuthOrchestratorTest.StubSessionManager(),
        new DirectCompletion(),
        java.util.Map.of()
    );

    AuthResult result = verifier.handle(AuthRequest.ofPassword("a@b.com", "Passw0rd!"), context);

    assertThat(result.error().code()).isEqualTo(AuthErrorCode.INVALID_PASSWORD);
  }

  @Test
  @DisplayName("비밀번호가 일치하면 사용자 정보를 반환한다")
  void returnsSuccessWhenPasswordMatches() {
    User user = newUser();
    UserContextManager userContextManager = new StubUserContextManager(Optional.of(user));
    PasswordAuthVerifier verifier = new PasswordAuthVerifier(new AuthService(new MatchingPasswordHasher()));
    AuthContext context = new AuthContext(
        Authentication.PASSWORD,
        CompletionStrategyClassification.DIRECT,
        userContextManager,
        new AuthOrchestratorTest.StubSessionManager(),
        new DirectCompletion(),
        java.util.Map.of()
    );

    AuthResult result = verifier.handle(AuthRequest.ofPassword("a@b.com", "Passw0rd!"), context);

    assertThat(result.success()).isTrue();
    assertThat(result.user().userId()).isEqualTo(user.getId());
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
        "enc",
        UserPasswordGuard.defaultPolicy("Passw0rd!")
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
      if (viewType.equals(User.class)) {
        return user.map(viewType::cast);
      }
      if (viewType.equals(UserAccountView.class)) {
        return user.map(UserAccountView::from).map(viewType::cast);
      }
      return Optional.empty();
    }
  }

  static final class MatchingPasswordHasher implements PasswordHasher {
    @Override
    public boolean matches(String rawPassword, PasswordHash hash) {
      return true;
    }
  }

  static final class RejectingPasswordHasher implements PasswordHasher {
    @Override
    public boolean matches(String rawPassword, PasswordHash hash) {
      return false;
    }
  }
}
