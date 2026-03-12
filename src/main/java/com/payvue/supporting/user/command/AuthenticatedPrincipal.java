package com.payvue.supporting.user.command;

public sealed interface AuthenticatedPrincipal
    permits PasswordPrincipal, OAuthPrincipal {
  String email();
}
