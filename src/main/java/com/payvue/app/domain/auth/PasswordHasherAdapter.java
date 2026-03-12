package com.payvue.app.domain.auth;

import com.payvue.app.domain.security.PasswordHash;
import org.springframework.security.crypto.password.PasswordEncoder;

class PasswordHasherAdapter implements PasswordHasher {
  private final PasswordEncoder passwordEncoder;

  public PasswordHasherAdapter(PasswordEncoder passwordEncoder) {
    this.passwordEncoder = passwordEncoder;
  }

  @Override
  public boolean matches(String rawPassword, PasswordHash hash) {
    if (rawPassword == null || hash == null || hash.value() == null) {
      return false;
    }
    return passwordEncoder.matches(rawPassword, hash.value());
  }
}
