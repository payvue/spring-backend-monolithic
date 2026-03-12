package com.payvue.app.domain.auth;

import java.util.Objects;

public record AuthRequest(
    String identifier,
    String secret,
    VerificationPurpose purpose
) {
  public static AuthRequest ofPassword(String email, String password) {
    return new AuthRequest(
        Objects.requireNonNull(email, "email"),
        Objects.requireNonNull(password, "password"),
        null
    );
  }

  public static AuthRequest ofEmailIssue(String email, VerificationPurpose purpose) {
    return new AuthRequest(
        Objects.requireNonNull(email, "email"),
        null,
        Objects.requireNonNull(purpose, "purpose")
    );
  }

  public static AuthRequest ofEmailVerify(String token, VerificationPurpose purpose) {
    return new AuthRequest(
        null,
        Objects.requireNonNull(token, "token"),
        Objects.requireNonNull(purpose, "purpose")
    );
  }

  public static AuthRequest ofLogout(String email) {
    return new AuthRequest(
            Objects.requireNonNull(email, "email"),
            null,
            null
    );
  }



  public static AuthRequest ofOAuth(String provider, String providerId, String email) {
    if (email == null) {
      return new AuthRequest(null, providerId, null);
    }
    String secret = provider == null ? providerId : provider + ":" + providerId;
    return new AuthRequest(email, secret, null);
  }
}
