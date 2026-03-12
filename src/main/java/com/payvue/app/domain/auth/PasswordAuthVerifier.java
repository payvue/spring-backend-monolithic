package com.payvue.app.domain.auth;

import com.payvue.supporting.user.UserContextManager;
import com.payvue.supporting.user.command.PasswordPrincipal;
import com.payvue.supporting.user.command.User;
import com.payvue.supporting.user.command.UserAccountView;
import java.util.Objects;

class PasswordAuthVerifier implements AuthSequenceHandler {
  private final AuthService authService;

  public PasswordAuthVerifier(AuthService authService) {
    this.authService = authService;
  }

  @Override
  public AuthResult handle(AuthRequest request, AuthContext context) {
    Objects.requireNonNull(request, "request");
    Objects.requireNonNull(context, "context");
    UserContextManager userContext = Objects.requireNonNull(context.userContextManager(), "userContext");
    User user = userContext.findByEmail(request.identifier(), User.class)
        .orElseThrow(() -> new IllegalArgumentException("user not found"));
    PasswordPrincipal authenticatedUser = PasswordPrincipal.from(user);
    if (!authService.authenticate(authenticatedUser, request.secret())) {
      return AuthResult.failure(AuthErrorCode.INVALID_PASSWORD);
    }
    return AuthResult.success(UserAccountView.from(user));
  }
}
