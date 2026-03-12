package com.payvue.app.domain.auth;

import com.payvue.app.domain.security.SessionTokenRepository;
import com.payvue.app.domain.security.TokenProvider;
import java.util.Objects;

final class SessionConfigurer {
  private SessionTokenRepository repository;
  private TokenProvider provider;

  SessionConfigurer repository(SessionTokenRepository repository) {
    this.repository = repository;
    return this;
  }

  SessionConfigurer provider(TokenProvider provider) {
    this.provider = provider;
    return this;
  }

  SessionManager build() {
    Objects.requireNonNull(repository, "session repository is required");
    Objects.requireNonNull(provider, "token provider is required");
    return new TokenSessionManager(provider, repository);
  }
}
