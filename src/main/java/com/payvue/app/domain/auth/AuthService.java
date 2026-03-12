package com.payvue.app.domain.auth;

import com.payvue.supporting.user.command.PasswordPrincipal;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class AuthService {

  private final PasswordHasher passwordHasher;

  public boolean authenticate(PasswordPrincipal authenticatedUser, String rawPassword) {
    if (authenticatedUser == null || rawPassword == null) {
      return false;
    }
    return passwordHasher.matches(rawPassword, authenticatedUser.passwordHash());
  }
}
