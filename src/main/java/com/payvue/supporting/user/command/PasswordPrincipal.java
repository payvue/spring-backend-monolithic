package com.payvue.supporting.user.command;

import com.payvue.app.domain.security.PasswordHash;

public record PasswordPrincipal(
    String email,
    PasswordHash passwordHash
) implements AuthenticatedPrincipal {

  public static PasswordPrincipal from(User user) {
    return new PasswordPrincipal(
        user.getAccount().getEmail(),
        new PasswordHash(user.getCredentials().getCurrent())
    );
  }
}
