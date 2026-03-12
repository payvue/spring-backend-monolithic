package com.payvue.app.domain.auth;

final class NextStepCompletionStrategy implements CompletionStrategy {
  private final CompletionStrategy delegate;
  private final NextStep nextStep;

  NextStepCompletionStrategy(CompletionStrategy delegate, NextStep nextStep) {
    this.delegate = delegate;
    this.nextStep = nextStep;
  }

  @Override
  public CompletionStrategyClassification type() {
    return delegate.type();
  }

  @Override
  public AuthCompletion complete(AuthContext context, AuthResult result) {
    if (nextStep == null) {
      return delegate.complete(context, result);
    }
    return nextStep.apply(result, context, delegate);
  }
}
