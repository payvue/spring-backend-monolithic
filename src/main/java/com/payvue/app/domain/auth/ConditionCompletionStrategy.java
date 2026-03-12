package com.payvue.app.domain.auth;

import java.util.List;

final class ConditionCompletionStrategy implements CompletionStrategy {
  private final CompletionStrategyClassification type;
  private final List<CompletionRule> rules;

  ConditionCompletionStrategy(
      CompletionStrategyClassification type,
      List<CompletionRule> rules
  ) {
    this.type = type;
    this.rules = rules;
  }

  @Override
  public CompletionStrategyClassification type() {
    return type;
  }

  @Override
  public AuthCompletion complete(AuthContext context, AuthResult result) {
    for (CompletionRule rule : rules) {
      if (rule.matches(context, result)) {
        return rule.strategy().complete(context, result);
      }
    }

    return new DirectCompletionResult(AuthResult.failure(AuthErrorCode.INVALID_REQUEST));
  }

  record CompletionRule(
      CompletionCondition condition,
      CompletionStrategy strategy
  ) {
    boolean matches(AuthContext context, AuthResult result) {
      return condition.test(context, result);
    }
  }

  @FunctionalInterface
  interface CompletionCondition {
    boolean test(AuthContext context, AuthResult result);
  }
}
