package com.payvue.app.domain.auth;

public interface CompletionStrategy {

  CompletionStrategyClassification type();
  AuthCompletion complete(AuthContext context, AuthResult result);
}
