package com.payvue.app.domain.auth;

class RedirectCompletion implements CompletionStrategy {
  @Override
  public CompletionStrategyClassification type() {
    return CompletionStrategyClassification.REDIRECT;
  }

  @Override
  public AuthCompletion complete(AuthContext context, AuthResult result) {
    if (result == null || result.isFailure()) {
      return new RedirectCompletionResult(result, null);
    }
    Object urlValue = result.meta().get(RedirectRequired.REDIRECT_URL);
    return new RedirectCompletionResult(result, (String) urlValue);
  }
}
