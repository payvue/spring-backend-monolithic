package com.payvue.app.domain.auth;

final class StrategyConfigurer {
  private CompletionStrategy completionStrategy;
  private NextStep nextStep;

  StrategyConfigurer complete(CompletionStrategy completionStrategy) {
    this.completionStrategy = completionStrategy;
    return this;
  }

  StrategyConfigurer next(NextStep nextStep) {
    this.nextStep = nextStep;
    return this;
  }

  CompletionStrategy build() {
    if (completionStrategy == null) {
      throw new IllegalStateException("completionStrategy is required");
    }
    if (nextStep == null) {
      return completionStrategy;
    }
    return new NextStepCompletionStrategy(completionStrategy, nextStep);
  }
}
