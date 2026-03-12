package com.payvue.app.domain.auth;

public enum AuthErrorCode {
  INVALID_REQUEST("invalid request"),
  USER_NOT_FOUND("user not found"),
  INVALID_PASSWORD("invalid password"),
  INVALID_TOKEN("invalid token"),
  UNSUPPORTED_AUTH("unsupported authentication");

  private final String message;

  AuthErrorCode(String message) {
    this.message = message;
  }

  public String message() {
    return message;
  }
}
