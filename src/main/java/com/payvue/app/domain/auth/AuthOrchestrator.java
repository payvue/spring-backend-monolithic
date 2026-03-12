package com.payvue.app.domain.auth;

class AuthOrchestrator {
  private final AuthContext context;

  public AuthOrchestrator(AuthContext context) {
    this.context = context;
  }

  public AuthCompletion execute(AuthRequest request, CompletionStrategyClassification.Sequence sequence) {
    if (request == null) {
      AuthResult result = AuthResult.failure(AuthErrorCode.INVALID_REQUEST);
      return context.completionStrategy().complete(context, result);
    }
    if (sequence == null) {
      AuthResult result = AuthResult.failure(AuthErrorCode.INVALID_REQUEST);
      return context.completionStrategy().complete(context, result);
    }
    AuthResult result = sequence.execute(this, request);
    if (result != null) {
      result = result.withMeta(SequenceByNextStep.SEQUENCE_META_KEY, sequence);
    }

    if(result.success() && sequence == CompletionStrategyClassification.Sequence.VERIFIFY) {
      AuthSession session = context.sessionManager().maintain(result.user());
      result = result.withMeta(SessionManager.SESSION_META_KEY, session);
    }

    if(result.isFailure()) {
      if (result.user() == null
          && request != null
          && request.identifier() != null
          && !request.identifier().isBlank()
          && context.type() == Authentication.NONE) {
        com.payvue.supporting.user.command.UserAccountView view =
            new com.payvue.supporting.user.command.UserAccountView(
                null,
                null,
                request.identifier(),
                null,
                false,
                null,
                null,
                null,
                false,
                null,
                java.util.Map.of()
            );
        context.sessionManager().suspend(view);
      } else {
        context.sessionManager().suspend(result.user());
      }
    }
    return context.completionStrategy().complete(context, result);
  }

  AuthResult verify(AuthRequest request) {
    return handle(CompletionStrategyClassification.Sequence.VERIFIFY, request);
  }

  AuthResult issue(AuthRequest request) {
    return handle(CompletionStrategyClassification.Sequence.ISSUE, request);
  }

  private AuthResult handle(CompletionStrategyClassification.Sequence sequence, AuthRequest request) {
    AuthSequenceHandler handler = context.handlers().get(sequence);
    if (handler == null) {
      return AuthResult.failure(AuthErrorCode.INVALID_REQUEST);
    }
    return handler.handle(request, context);
  }

}
