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

class EmailAuthFlowTest {

  @Test
  @DisplayName("이메일 토큰 발급은 리다이렉트 URL 메타를 포함한다")
  void issueIncludesRedirectUrl() {
    InMemoryTokenRepository repository = new InMemoryTokenRepository();
    VerificationProperties properties = new VerificationProperties();
    properties.setBaseUrl("https://example.com");
    properties.setEndpoint("/verify");
    VerificationTokenService service = new VerificationTokenService(repository, properties);
    EmailTokenIssuer issuer = new EmailTokenIssuer(service);

    User user = newUser();
    AuthContext context = new AuthContext(
        Authentication.EMAIL,
        CompletionStrategyClassification.REDIRECT,
        new StubUserContextManager(Optional.of(user)),
        new AuthOrchestratorTest.StubSessionManager(),
        new RedirectCompletion(),
        java.util.Map.of()
    );

    AuthResult result = issuer.handle(AuthRequest.ofEmailIssue("a@b.com", VerificationPurpose.SIGNUP), context);

    assertThat(result.success()).isTrue();
    assertThat(result.meta()).containsKey(RedirectRequired.REDIRECT_URL);
  }

  @Test
  @DisplayName("이메일 토큰 검증은 사용자 정보를 반환한다")
  void verifyReturnsUserView() {
    InMemoryTokenRepository repository = new InMemoryTokenRepository();
    VerificationProperties properties = new VerificationProperties();
    properties.setBaseUrl("https://example.com");
    properties.setEndpoint("/verify");
    VerificationTokenService service = new VerificationTokenService(repository, properties);
    EmailTokenVerifier verifier = new EmailTokenVerifier(service);

    User user = newUser();
    VerificationToken token = service.generateToken(user, VerificationPurpose.SIGNUP);

    AuthContext context = new AuthContext(
        Authentication.EMAIL,
        CompletionStrategyClassification.REDIRECT,
        new StubUserContextManager(Optional.of(user)),
        new AuthOrchestratorTest.StubSessionManager(),
        new RedirectCompletion(),
        java.util.Map.of()
    );

    AuthResult result = verifier.handle(
        AuthRequest.ofEmailVerify(token.getToken(), VerificationPurpose.SIGNUP),
        context
    );

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
      if (viewType.equals(User.class)) {
        return user.map(viewType::cast);
      }
      if (viewType.equals(UserAccountView.class)) {
        return user.map(UserAccountView::from).map(viewType::cast);
      }
      return Optional.empty();
    }
  }

  static final class InMemoryTokenRepository implements VerificationTokenRepository {
    private VerificationToken stored;

    @Override
    public void save(VerificationToken token) {
      stored = token;
    }

    @Override
    public Optional<VerificationToken> findByToken(String token, VerificationPurpose purpose) {
      if (stored == null) {
        return Optional.empty();
      }
      if (stored.getToken().equals(token) && stored.getPurpose() == purpose) {
        return Optional.of(stored);
      }
      return Optional.empty();
    }

    @Override
    public Optional<VerificationToken> findByIdentifier(String identifier, VerificationPurpose purpose) {
      if (stored == null) {
        return Optional.empty();
      }
      if (stored.getIdentifier().equals(identifier) && stored.getPurpose() == purpose) {
        return Optional.of(stored);
      }
      return Optional.empty();
    }

    @Override
    public void delete(String identifier, VerificationPurpose purpose) {
      if (stored != null && stored.getIdentifier().equals(identifier) && stored.getPurpose() == purpose) {
        stored = null;
      }
    }

    @Override
    public boolean exists(String identifier, VerificationPurpose purpose) {
      return stored != null && stored.getIdentifier().equals(identifier) && stored.getPurpose() == purpose;
    }
  }
}
