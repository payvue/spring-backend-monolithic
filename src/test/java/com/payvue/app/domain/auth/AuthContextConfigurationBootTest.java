package com.payvue.app.domain.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.payvue.app.domain.security.SessionTokenRepository;
import com.payvue.app.domain.security.TokenProvider;
import com.payvue.supporting.user.UserContextManager;
import com.payvue.supporting.user.command.User;
import com.payvue.supporting.user.command.UserPasswordGuard;
import com.payvue.supporting.user.command.UserAccountView;
import com.payvue.supporting.user.command.UserId;
import java.time.LocalDate;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest(classes = {
    AuthContextConfiguration.class,
    AuthContextConfigurationBootTest.StubConfig.class
})
class AuthContextConfigurationBootTest {

  @Autowired
  private AuthContextManager authContextManager;
  @Autowired
  private UserContextManager userContextManager;
  @Autowired
  private VerificationTokenService verificationTokenService;
  @Autowired
  private PasswordEncoder passwordEncoder;

  @Test
  @DisplayName("NONE 인증 요청은 DirectCompletionResult로 응답한다")
  void authenticateNoneReturnsDirectCompletion() {
    AuthCompletion completion = authContextManager.authenticate(
        Authentication.NONE,
        CompletionStrategyClassification.DIRECT,
        CompletionStrategyClassification.Sequence.VERIFIFY,
        AuthRequest.ofLogout("a@b.com")
    );

    assertThat(completion).isInstanceOf(DirectCompletionResult.class);
    assertThat(completion.result().error().code()).isEqualTo(AuthErrorCode.UNSUPPORTED_AUTH);
    assertThat(completion.result().success()).isFalse();
    assertThat(completion.result().user()).isNull();
  }

  @Test
  @DisplayName("지원되지 않는 인증 타입은 UNSUPPORTED_AUTH로 응답한다")
  void authenticateReturnsUnsupportedWhenMissingFlow() {
    AuthCompletion completion = authContextManager.authenticate(
        Authentication.PASSWORD,
        CompletionStrategyClassification.REDIRECT,
        CompletionStrategyClassification.Sequence.ISSUE,
        AuthRequest.ofEmailIssue("a@b.com", VerificationPurpose.SIGNUP)
    );

    assertThat(completion.result().error().code()).isEqualTo(AuthErrorCode.UNSUPPORTED_AUTH);
    assertThat(completion.result().success()).isFalse();
  }

  @Test
  @DisplayName("전략이 시퀀스를 지원하지 않으면 INVALID_REQUEST로 응답한다")
  void authenticateReturnsInvalidRequestWhenSequenceNotSupported() {
    AuthCompletion completion = authContextManager.authenticate(
        Authentication.EMAIL,
        CompletionStrategyClassification.DIRECT,
        CompletionStrategyClassification.Sequence.ISSUE,
        AuthRequest.ofEmailIssue("a@b.com", VerificationPurpose.SIGNUP)
    );

    assertThat(completion.result().error().code()).isEqualTo(AuthErrorCode.UNSUPPORTED_AUTH);
    assertThat(completion.result().success()).isFalse();
  }

  @Test
  @DisplayName("PASSWORD 인증은 DirectCompletionResult로 성공한다")
  void authenticatePasswordSuccess() {
    User user = newUserWithPassword();
    ((StubConfig.StubUserContextManager) userContextManager).setUser(user);

    AuthCompletion completion = authContextManager.authenticate(
        Authentication.PASSWORD,
        CompletionStrategyClassification.DIRECT,
        CompletionStrategyClassification.Sequence.VERIFIFY,
        AuthRequest.ofPassword("a@b.com", "Passw0rd!")
    );

    assertThat(completion).isInstanceOf(DirectCompletionResult.class);
    assertThat(completion.result().success()).isTrue();
    assertThat(completion.result().user()).isNotNull();
    assertThat(completion.result().user().email()).isEqualTo("a@b.com");
    assertThat(completion.result().meta()).containsKey(SessionManager.SESSION_META_KEY);
  }

  @Test
  @DisplayName("EMAIL 인증 토큰 발급은 RedirectCompletionResult로 응답한다")
  void authenticateEmailIssueSuccess() {
    User user = newUser();
    ((StubConfig.StubUserContextManager) userContextManager).setUser(user);

    AuthCompletion completion = authContextManager.authenticate(
        Authentication.EMAIL,
        CompletionStrategyClassification.REDIRECT,
        CompletionStrategyClassification.Sequence.ISSUE,
        AuthRequest.ofEmailIssue("a@b.com", VerificationPurpose.SIGNUP)
    );

    assertThat(completion).isInstanceOf(RedirectCompletionResult.class);
    RedirectCompletionResult redirect = (RedirectCompletionResult) completion;
    assertThat(redirect.redirectUrl()).startsWith("http://localhost:8080/api/auth/verify?token=");
    assertThat(redirect.redirectUrl()).contains("&purpose=SIGNUP");
    assertThat(redirect.result().user()).isNotNull();
    assertThat(redirect.result().user().email()).isEqualTo("a@b.com");
    assertThat(redirect.result().meta()).containsKey(RedirectRequired.REDIRECT_URL);
    assertThat(redirect.result().meta()).doesNotContainKey(SessionManager.SESSION_META_KEY);
  }

  @Test
  @DisplayName("EMAIL 인증 토큰 검증은 RedirectCompletionResult로 성공한다")
  void authenticateEmailVerifySuccess() {
    User user = newUser();
    ((StubConfig.StubUserContextManager) userContextManager).setUser(user);
    VerificationToken token = verificationTokenService.generateToken(user, VerificationPurpose.SIGNUP);

    AuthCompletion completion = authContextManager.authenticate(
        Authentication.EMAIL,
        CompletionStrategyClassification.REDIRECT,
        CompletionStrategyClassification.Sequence.VERIFIFY,
        AuthRequest.ofEmailVerify(token.getToken(), VerificationPurpose.SIGNUP)
    );

    assertThat(completion).isInstanceOf(RedirectCompletionResult.class);
    assertThat(completion.result().success()).isTrue();
    assertThat(completion.result().user()).isNotNull();
    assertThat(completion.result().user().email()).isEqualTo("a@b.com");
    assertThat(((RedirectCompletionResult) completion).redirectUrl()).isNull();
    assertThat(completion.result().meta()).containsKey(SessionManager.SESSION_META_KEY);
  }

  @Test
  @DisplayName("OAUTH 인증은 사용자 존재 시 DirectCompletionResult로 성공한다")
  void authenticateOAuthSuccess() {
    User user = newUser();
    ((StubConfig.StubUserContextManager) userContextManager).setUser(user);

    AuthCompletion completion = authContextManager.authenticate(
        Authentication.OAUTH2,
        CompletionStrategyClassification.OAUTH_DYNAMIC,
        CompletionStrategyClassification.Sequence.VERIFIFY,
        AuthRequest.ofOAuth("google", "id", "a@b.com")
    );

    assertThat(completion).isInstanceOf(DirectCompletionResult.class);
    assertThat(completion.result().success()).isTrue();
    assertThat(completion.result().user()).isNotNull();
    assertThat(completion.result().user().email()).isEqualTo("a@b.com");
    assertThat(completion.result().meta()).containsKey(SessionManager.SESSION_META_KEY);
  }

  @TestConfiguration
  static class StubConfig {
    @Bean
    VerificationTokenRepository verificationTokenRepository() {
      return new FixedTokenRepository();
    }

    @Bean
    VerificationProperties verificationProperties() {
      VerificationProperties properties = new VerificationProperties();
      properties.setBaseUrl("https://example.com");
      properties.setEndpoint("/verify");
      return properties;
    }

    @Bean
    UserContextManager userContextManager() {
      return new StubUserContextManager();
    }

    @Bean
    TokenProvider tokenProvider() {
      return new TokenProvider() {
        @Override
        public String generate(com.payvue.supporting.user.command.AuthenticatedPrincipal authenticatedPrincipal, String type) {
          return "token";
        }

        @Override
        public boolean validate(String token) {
          return true;
        }

        @Override
        public String getSubject(String token) {
          return "subject";
        }

        @Override
        public Map<String, Object> getClaims(String token) {
          return Map.of();
        }
      };
    }

    @Bean
    SessionTokenRepository sessionTokenRepository() {
      return new SessionTokenRepository() {
        @Override
        public void save(String token, String subject, Duration ttl) {}

        @Override
        public boolean isActive(String token) {
          return true;
        }

        @Override
        public void remove(String token) {}

        @Override
        public void delete(User user) {}

        @Override
        public void delete(UserAccountView view) {}
      };
    }

    static final class FixedTokenRepository implements VerificationTokenRepository {
      @Override
      public void save(VerificationToken token) {}

      @Override
      public Optional<VerificationToken> findByToken(String token, VerificationPurpose purpose) {
        return Optional.of(VerificationToken.reconstitute(
            token,
            purpose,
            "a@b.com",
            java.time.Instant.now(),
            Map.of()
        ));
      }

      @Override
      public Optional<VerificationToken> findByIdentifier(String identifier, VerificationPurpose purpose) {
        return Optional.of(VerificationToken.reconstitute(
            "token",
            purpose,
            identifier,
            java.time.Instant.now(),
            Map.of()
        ));
      }

      @Override
      public void delete(String identifier, VerificationPurpose purpose) {}

      @Override
      public boolean exists(String identifier, VerificationPurpose purpose) {
        return true;
      }
    }

    static final class StubUserContextManager implements UserContextManager {
      private User user;

      void setUser(User user) {
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
        if (user == null) {
          return Optional.empty();
        }
        if (viewType.equals(User.class)) {
          return Optional.of(viewType.cast(user));
        }
        if (viewType.equals(UserAccountView.class)) {
          return Optional.of(viewType.cast(UserAccountView.from(user)));
        }
        return Optional.empty();
      }
    }
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

  private User newUserWithPassword() {
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
        passwordEncoder.encode("Passw0rd!"),
        UserPasswordGuard.defaultPolicy("Passw0rd!")
    );
  }
}
