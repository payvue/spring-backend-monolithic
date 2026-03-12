package com.payvue.app.domain.auth;

public interface AuthContextManager {

  <T extends AuthCompletion> T authenticate(
      Authentication type,
      CompletionStrategyClassification strategy,
      CompletionStrategyClassification.Sequence sequence,
      AuthRequest request
  );

  <T extends AuthCompletion> T deauthenticate(
          AuthRequest request
  );
}
