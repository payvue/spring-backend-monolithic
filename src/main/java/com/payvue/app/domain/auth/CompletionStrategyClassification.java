package com.payvue.app.domain.auth;

public enum CompletionStrategyClassification {
  DIRECT(DirectCompletionResult.class) {
    @Override
    AuthCompletion failure(AuthErrorCode code) {
      return new DirectCompletionResult(AuthResult.failure(code));
    }

    @Override
    public boolean supports(Sequence sequence) {
      return sequence == Sequence.VERIFIFY;
    }
  },
  REDIRECT(RedirectCompletionResult.class) {
    @Override
    AuthCompletion failure(AuthErrorCode code) {
      return new RedirectCompletionResult(AuthResult.failure(code), null);
    }

    @Override
    public boolean supports(Sequence sequence) {
      return sequence == Sequence.ISSUE || sequence == Sequence.VERIFIFY;
    }
  },
  OAUTH_DYNAMIC(AuthCompletion.class) {
    @Override
    AuthCompletion failure(AuthErrorCode code) {
      return new RedirectCompletionResult(AuthResult.failure(code), null);
    }

    @Override
    public boolean supports(Sequence sequence) {
      return sequence == Sequence.VERIFIFY;
    }
  };

  public enum Sequence {
    ISSUE(AuthOrchestrator::issue),
    VERIFIFY(AuthOrchestrator::verify);

    private final java.util.function.BiFunction<AuthOrchestrator, AuthRequest, AuthResult> executor;

    Sequence(java.util.function.BiFunction<AuthOrchestrator, AuthRequest, AuthResult> executor) {
      this.executor = executor;
    }

    public AuthResult execute(AuthOrchestrator flow, AuthRequest request) {
      return executor.apply(flow, request);
    }
  }

  private final Class<? extends AuthCompletion> resultType;

  CompletionStrategyClassification(Class<? extends AuthCompletion> resultType) {
    this.resultType = resultType;
  }

  public Class<? extends AuthCompletion> resultType() {
    return resultType;
  }

  public static AuthCompletion failure(CompletionStrategyClassification strategy, AuthErrorCode code) {
    return strategy.failure(code);
  }

  abstract AuthCompletion failure(AuthErrorCode code);

  public boolean supports(Sequence sequence) {
    return false;
  }
}
