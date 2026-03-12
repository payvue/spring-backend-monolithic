package com.payvue.app.domain.auth;

public record RedirectCompletionResult(
    AuthResult result,
    String redirectUrl
) implements AuthCompletion {}
