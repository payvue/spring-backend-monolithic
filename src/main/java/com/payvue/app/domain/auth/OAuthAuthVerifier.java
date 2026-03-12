package com.payvue.app.domain.auth;

import com.payvue.supporting.user.UserContextManager;
import com.payvue.supporting.user.command.UserAccountView;
import java.util.Objects;

class OAuthAuthVerifier implements AuthSequenceHandler {

  private final UserContextManager userContextManager;
  private final String REDIRECT_URL = "auth/complete-profile";

  OAuthAuthVerifier(UserContextManager userContextManager) {
    this.userContextManager = userContextManager;
  }

  @Override
  public AuthResult handle(AuthRequest request, AuthContext context) {
    Objects.requireNonNull(request, "request");
    String email = request.identifier();
    if (email == null || email.isBlank()) {
      return AuthResult.failure(AuthErrorCode.INVALID_REQUEST);
    }

    boolean exist = userContextManager.findByEmail(email, UserAccountView.class).isPresent();
    if(exist) {
      UserAccountView view = userContextManager.findByEmail(email, UserAccountView.class)
              .orElseThrow(IllegalAccessError::new);
      return AuthResult.success(view);
    }

    return AuthResult.failure(AuthErrorCode.USER_NOT_FOUND)
            .withMeta(RedirectRequired.REDIRECT_URL, REDIRECT_URL);

  }
}
