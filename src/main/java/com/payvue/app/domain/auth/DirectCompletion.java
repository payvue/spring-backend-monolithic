package com.payvue.app.domain.auth;

class DirectCompletion implements CompletionStrategy {
  @Override
  public CompletionStrategyClassification type() {
    return CompletionStrategyClassification.DIRECT;
  }

  @Override
  public AuthCompletion complete(AuthContext context, AuthResult result) {
    return new DirectCompletionResult(result);
  }
}
