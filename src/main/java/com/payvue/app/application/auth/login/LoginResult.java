package com.payvue.app.application.auth.login;

import com.payvue.app.domain.auth.AuthSession;

public record LoginResult(String accessToken, String refreshToken) {
  public static LoginResult from(AuthSession session) {
    if (session == null) {
      return new LoginResult(null, null);
    }
    return new LoginResult(session.accessToken(), session.refreshToken());
  }
}
