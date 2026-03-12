package com.payvue.app.application.auth.verify;

import com.payvue.app.application.auth.recovery.FindIdResult;
import com.payvue.app.domain.auth.AuthContextManager;
import com.payvue.app.domain.auth.AuthRequest;
import com.payvue.app.domain.auth.AuthResult;
import com.payvue.app.domain.auth.Authentication;
import com.payvue.app.domain.auth.CompletionStrategyClassification;
import com.payvue.app.domain.auth.RedirectCompletionResult;
import com.payvue.app.domain.auth.VerificationPurpose;
import com.payvue.app.domain.auth.VerificationTokenService;
import com.payvue.supporting.user.UserContextManager;
import com.payvue.supporting.user.command.User;
import com.payvue.supporting.user.command.UserAccountView;
import java.util.function.Consumer;

public final class VerificationFindIdFlow {
  private final VerificationFindIdContext context;

  private VerificationFindIdFlow(VerificationFindIdContext context) {
    this.context = context;
  }

  public static VerificationFindIdFlow start(String token) {
    return new VerificationFindIdFlow(new VerificationFindIdContext(token));
  }

  public VerificationFindIdFlow token(Consumer<VerificationFindIdFlow> step) {
    step.accept(this);
    return this;
  }

  public VerificationFindIdFlow auth(Consumer<VerificationFindIdFlow> step) {
    step.accept(this);
    return this;
  }

  public VerificationFindIdFlow user(Consumer<VerificationFindIdFlow> step) {
    step.accept(this);
    return this;
  }

  public VerificationFindIdFlow map(Consumer<VerificationFindIdFlow> step) {
    step.accept(this);
    return this;
  }

  public VerificationFindIdFlow verify(AuthContextManager authContextManager) {
    RedirectCompletionResult completion = authContextManager.authenticate(
        Authentication.EMAIL,
        CompletionStrategyClassification.REDIRECT,
        CompletionStrategyClassification.Sequence.VERIFIFY,
        AuthRequest.ofEmailVerify(context.token(), VerificationPurpose.FIND_ID)
    );
    AuthResult result = completion.result();
    if (result == null || result.isFailure() || result.user() == null) {
      throw new IllegalArgumentException(result == null ? "invalid token" : result.error().message());
    }
    context.accountView(result.user());
    return this;
  }

  public VerificationFindIdFlow loadUser(UserContextManager userContextManager) {
    UserAccountView view = context.accountView();
    String email = view == null ? null : view.email();
    if (email == null || email.isBlank()) {
      throw new IllegalArgumentException("유효하지 않은 인증 정보입니다.");
    }
    User user = userContextManager.findByEmail(email, User.class)
        .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    context.user(user);
    return this;
  }

  public VerificationFindIdFlow toResult() {
    UserAccountView view = UserAccountView.from(context.user());
    context.result(new FindIdResult(
        view.loginId(),
        maskEmail(view.email())
    ));
    return this;
  }

  public FindIdResult end() {
    return context.result();
  }

  private String maskEmail(String email) {
    String[] parts = email.split("@");
    if (parts.length != 2) {
      return email;
    }
    String local = parts[0];
    String domain = parts[1];
    if (local.length() <= 2) {
      return local.charAt(0) + "***@" + domain;
    }
    return local.substring(0, 2) + "***@" + domain;
  }
}
