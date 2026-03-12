package com.payvue.app.domain.auth;

import com.payvue.app.domain.security.SessionTokenRepository;
import com.payvue.app.domain.security.TokenProvider;
import com.payvue.supporting.user.command.OAuthPrincipal;
import com.payvue.supporting.user.command.UserAccountView;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

final class TokenSessionManager implements SessionManager {
  private final TokenProvider tokenProvider;
  private final SessionTokenRepository sessionTokenRepository;

  TokenSessionManager(TokenProvider tokenProvider, SessionTokenRepository sessionTokenRepository) {
    this.tokenProvider = tokenProvider;
    this.sessionTokenRepository = sessionTokenRepository;
  }

  @Override
  public AuthSession maintain(UserAccountView user) {
    if (user == null || user.email() == null) {
      return new AuthSession(null, null);
    }
    OAuthPrincipal principal = new OAuthPrincipal("local", user.email(), user.email());
    String accessToken = tokenProvider.generate(principal, "ACCESS");
    String refreshToken = tokenProvider.generate(principal, "REFRESH");
    Duration ttl = resolveTtl(tokenProvider, accessToken);
    sessionTokenRepository.save(accessToken, user.email(), ttl);
    return new AuthSession(accessToken, refreshToken);
  }

  @Override
  public void suspend(UserAccountView user) {
    if (user == null || user.email() == null || user.email().isBlank()) {
      return;
    }
    sessionTokenRepository.delete(user);
  }

  private Duration resolveTtl(TokenProvider tokenProvider, String token) {
    Map<String, Object> claims = tokenProvider.getClaims(token);
    Object exp = claims.get("exp");
    Instant expiresAt = null;
    if (exp instanceof Date date) {
      expiresAt = date.toInstant();
    } else if (exp instanceof Number number) {
      expiresAt = Instant.ofEpochSecond(number.longValue());
    }
    if (expiresAt == null) {
      return Duration.ZERO;
    }
    Duration ttl = Duration.between(Instant.now(), expiresAt);
    return ttl.isNegative() ? Duration.ZERO : ttl;
  }
}
