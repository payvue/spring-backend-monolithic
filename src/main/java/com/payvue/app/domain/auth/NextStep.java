package com.payvue.app.domain.auth;

interface NextStep {
  AuthCompletion apply(AuthResult result, AuthContext context, CompletionStrategy delegate);
}
