package com.payvue.app.domain.auth;

import com.payvue.supporting.user.command.UserAccountView;

public record AuthResult(
    boolean success,
    UserAccountView user,
    AuthError error,
    java.util.Map<String, Object> meta
) {
  public static AuthResult success(UserAccountView user) {
    return new AuthResult(true, user, null, java.util.Map.of());
  }

  public static AuthResult failure(AuthErrorCode code) {
    return new AuthResult(false, null, AuthError.of(code), java.util.Map.of());
  }

  public AuthResult withMeta(String key, Object value) {
    java.util.Map<String, Object> next = new java.util.HashMap<>(meta);
    next.put(key, value);
    return new AuthResult(success, user, error, java.util.Map.copyOf(next));
  }

  public boolean isFailure() {
    return !success;
  }
}
