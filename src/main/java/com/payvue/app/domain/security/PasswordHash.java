package com.payvue.app.domain.security;

public final class PasswordHash {
  private final String value;

  public PasswordHash(String value) {
    this.value = value;
  }

  public String value() {
    return value;
  }
}
