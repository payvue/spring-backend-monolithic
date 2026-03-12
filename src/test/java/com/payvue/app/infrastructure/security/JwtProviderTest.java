package com.payvue.app.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.payvue.app.domain.security.PasswordHash;
import com.payvue.supporting.user.command.PasswordPrincipal;
import io.jsonwebtoken.Claims;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JwtProviderTest {

  @Test
  @DisplayName("JWT는 access 토큰 TTL을 사용한다")
  void generatesAccessTokenWithAccessTtl() {
    JwtProperties properties = properties();
    Clock clock = Clock.fixed(Instant.parse("2024-01-01T00:00:00Z"), ZoneOffset.UTC);
    JwtProvider provider = new JwtProvider(properties);

    String token = provider.generate(new PasswordPrincipal("a@b.com", new PasswordHash("hash")), "ACCESS");

    Claims claims = (Claims) provider.getClaims(token);
    assertThat(claims.getSubject()).isEqualTo("a@b.com");
    assertThat(claims.get("type")).isEqualTo("ACCESS");

    Duration ttl = Duration.between(claims.getIssuedAt().toInstant(), claims.getExpiration().toInstant());
    assertThat(ttl).isEqualTo(properties.getAccessTokenTtl());
  }

  @Test
  @DisplayName("JWT는 refresh 토큰 TTL을 사용한다")
  void generatesRefreshTokenWithRefreshTtl() {
    JwtProperties properties = properties();
    Clock clock = Clock.fixed(Instant.parse("2024-01-01T00:00:00Z"), ZoneOffset.UTC);
    JwtProvider provider = new JwtProvider(properties);

    String token = provider.generate(new PasswordPrincipal("a@b.com", new PasswordHash("hash")), "REFRESH");

    Claims claims = (Claims) provider.getClaims(token);
    Duration ttl = Duration.between(claims.getIssuedAt().toInstant(), claims.getExpiration().toInstant());

    assertThat(ttl).isEqualTo(properties.getRefreshTokenTtl());
  }

  @Test
  @DisplayName("JWT 검증은 잘못된 토큰을 거부한다")
  void validateRejectsInvalidToken() {
    JwtProvider provider = new JwtProvider(properties());
    String token = provider.generate(new PasswordPrincipal("a@b.com", new PasswordHash("hash")), "ACCESS");

    assertThat(provider.validate(token + "tamper")).isFalse();
  }

  @Test
  @DisplayName("JWT 검증은 정상 토큰을 승인한다")
  void validateAcceptsValidToken() {
    JwtProvider provider = new JwtProvider(properties());
    String token = provider.generate(new PasswordPrincipal("a@b.com", new PasswordHash("hash")), "ACCESS");

    assertThat(provider.validate(token)).isTrue();
  }

  @Test
  @DisplayName("JWT secret이 없으면 예외를 던진다")
  void throwsWhenSecretMissing() {
    JwtProperties properties = new JwtProperties();
    properties.setIssuer("issuer");
    properties.setAccessTokenTtl(Duration.ofMinutes(10));
    properties.setRefreshTokenTtl(Duration.ofMinutes(30));
    properties.setSecret(null);

    assertThatThrownBy(() -> new JwtProvider(properties))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("JWT secret");
  }

  private JwtProperties properties() {
    JwtProperties properties = new JwtProperties();
    properties.setIssuer("issuer");
    properties.setAccessTokenTtl(Duration.ofMinutes(15));
    properties.setRefreshTokenTtl(Duration.ofMinutes(60));
    properties.setSecret("12345678901234567890123456789012");
    return properties;
  }
}
