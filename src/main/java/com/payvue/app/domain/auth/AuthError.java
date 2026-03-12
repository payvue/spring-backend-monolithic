package com.payvue.app.domain.auth;

public record AuthError(AuthErrorCode code, String message) {
  public static AuthError of(AuthErrorCode code) {
    return new AuthError(code, code.message());
  }
}
