package com.payvue.app.domain.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.payvue.supporting.user.command.User;
import com.payvue.supporting.user.command.UserAccountView;
import com.payvue.supporting.user.command.UserId;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class VerificationTokenServiceTest {

  @Test
  @DisplayName("토큰 생성은 저장되고 검증 URL이 생성된다")
  void generateTokenAndUrl() {
    InMemoryTokenRepository repository = new InMemoryTokenRepository();
    VerificationProperties properties = new VerificationProperties();
    properties.setBaseUrl("https://example.com");
    properties.setEndpoint("/verify");
    VerificationTokenService service = new VerificationTokenService(repository, properties);
    User user = newUser();

    VerificationToken token = service.generateToken(user, VerificationPurpose.SIGNUP);
    String url = service.generateVerificationUrl(token);

    assertThat(repository.stored).isNotNull();
    assertThat(url).contains("https://example.com/verify");
    assertThat(url).contains("token=" + token.getToken());
  }

  @Test
  @DisplayName("토큰 검증은 저장된 토큰을 반환한다")
  void verifyTokenReturnsStoredToken() {
    InMemoryTokenRepository repository = new InMemoryTokenRepository();
    VerificationTokenService service = new VerificationTokenService(repository, new VerificationProperties());
    User user = newUser();

    VerificationToken token = service.generateToken(user, VerificationPurpose.SIGNUP);
    VerificationToken verified = service.verifyToken(token.getToken(), VerificationPurpose.SIGNUP);

    assertThat(verified.getToken()).isEqualTo(token.getToken());
  }

  @Test
  @DisplayName("존재하지 않는 토큰은 예외가 발생한다")
  void verifyTokenThrowsWhenMissing() {
    InMemoryTokenRepository repository = new InMemoryTokenRepository();
    VerificationTokenService service = new VerificationTokenService(repository, new VerificationProperties());

    assertThatThrownBy(() -> service.verifyToken("missing", VerificationPurpose.SIGNUP))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("verifyAndConsumeToken은 토큰을 소비한다")
  void verifyAndConsumeDeletesToken() {
    InMemoryTokenRepository repository = new InMemoryTokenRepository();
    VerificationTokenService service = new VerificationTokenService(repository, new VerificationProperties());
    User user = newUser();

    VerificationToken token = service.generateToken(user, VerificationPurpose.SIGNUP);
    VerificationToken verified = service.verifyAndConsumeToken(token.getToken(), VerificationPurpose.SIGNUP);

    assertThat(verified.getToken()).isEqualTo(token.getToken());
    assertThat(repository.stored).isNull();
  }

  @Test
  @DisplayName("findByIdentifier는 저장된 토큰을 조회한다")
  void findByIdentifierReturnsToken() {
    InMemoryTokenRepository repository = new InMemoryTokenRepository();
    VerificationTokenService service = new VerificationTokenService(repository, new VerificationProperties());
    User user = newUser();

    VerificationToken token = service.generateToken(user, VerificationPurpose.SIGNUP);
    Optional<VerificationToken> found = service.findByIdentifier(token.getIdentifier(), VerificationPurpose.SIGNUP);

    assertThat(found).isPresent();
    assertThat(found.get().getToken()).isEqualTo(token.getToken());
  }

  @Test
  @DisplayName("hasValidToken과 deleteToken은 저장 상태에 따라 동작한다")
  void hasValidTokenAndDeleteToken() {
    InMemoryTokenRepository repository = new InMemoryTokenRepository();
    VerificationTokenService service = new VerificationTokenService(repository, new VerificationProperties());
    User user = newUser();

    VerificationToken token = service.generateToken(user, VerificationPurpose.SIGNUP);
    assertThat(service.hasValidToken(token.getIdentifier(), VerificationPurpose.SIGNUP)).isTrue();

    service.deleteToken(token.getIdentifier(), VerificationPurpose.SIGNUP);

    assertThat(service.hasValidToken(token.getIdentifier(), VerificationPurpose.SIGNUP)).isFalse();
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

  static final class InMemoryTokenRepository implements VerificationTokenRepository {
    VerificationToken stored;

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
