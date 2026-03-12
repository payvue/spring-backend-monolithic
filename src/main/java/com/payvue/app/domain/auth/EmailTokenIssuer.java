package com.payvue.app.domain.auth;

import com.payvue.supporting.user.UserContextManager;
import com.payvue.supporting.user.command.User;
import com.payvue.supporting.user.command.UserAccountView;
import java.util.Objects;

class EmailTokenIssuer implements AuthSequenceHandler {
  private final VerificationTokenService verificationTokenService;

  public EmailTokenIssuer(VerificationTokenService verificationTokenService) {
    this.verificationTokenService = verificationTokenService;
  }

  @Override
  public AuthResult handle(AuthRequest request, AuthContext context) {
    Objects.requireNonNull(request, "request");
    Objects.requireNonNull(context, "context");
    UserContextManager userContext = Objects.requireNonNull(context.userContextManager(), "userContext");
    User user = userContext.findByEmail(request.identifier(), User.class)
        .orElseThrow(() -> new IllegalArgumentException("user not found"));
    VerificationToken token = verificationTokenService.generateToken(user, request.purpose());
    String url = verificationTokenService.generateVerificationUrl(token);
    return AuthResult.success(UserAccountView.from(user))
        .withMeta(RedirectRequired.REDIRECT_URL, url);
  }
}
