package com.payvue.app.domain.auth;

import com.payvue.supporting.user.UserContextManager;
import com.payvue.supporting.user.command.UserAccountView;
import java.util.Objects;

class EmailTokenVerifier implements AuthSequenceHandler {
  private final VerificationTokenService verificationTokenService;

  public EmailTokenVerifier(VerificationTokenService verificationTokenService) {
    this.verificationTokenService = verificationTokenService;
  }

  @Override
  public AuthResult handle(AuthRequest request, AuthContext context) {
    Objects.requireNonNull(request, "request");
    VerificationToken token;
    try {
      token = verificationTokenService.verifyToken(
          Objects.requireNonNull(request.secret(), "token"),
          Objects.requireNonNull(request.purpose(), "purpose")
      );
    } catch (IllegalArgumentException ex) {
      return AuthResult.failure(AuthErrorCode.INVALID_TOKEN);
    }
    UserContextManager userContext = Objects.requireNonNull(context.userContextManager(), "userContext");
    UserAccountView view = userContext.findByEmail(token.getIdentifier(), UserAccountView.class)
        .orElseThrow(() -> new IllegalArgumentException("user not found"));

    return AuthResult.success(view);
  }
}
