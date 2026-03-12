package com.payvue.app.domain.auth;

final class AuthenticateComplete implements NextStep {
  @Override
  public AuthCompletion apply(AuthResult result, AuthContext context, CompletionStrategy delegate) {
    return delegate.complete(context, result);
  }
}
