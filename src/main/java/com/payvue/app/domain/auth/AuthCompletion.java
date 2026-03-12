package com.payvue.app.domain.auth;

public sealed interface AuthCompletion
    permits DirectCompletionResult, RedirectCompletionResult {
  AuthResult result();
}
