package com.payvue.app.application.auth.verify;

import com.payvue.generic.alarm.AlarmContextManager;

import com.payvue.generic.alarm.AlarmFactory;
import com.payvue.app.domain.auth.AuthContextManager;
import com.payvue.app.domain.auth.AuthRequest;
import com.payvue.app.domain.auth.AuthResult;
import com.payvue.app.domain.auth.Authentication;
import com.payvue.app.domain.auth.CompletionStrategyClassification;
import com.payvue.app.domain.auth.RedirectCompletionResult;
import com.payvue.app.domain.auth.VerificationPurpose;
import com.payvue.app.domain.auth.VerificationTokenService;
import com.payvue.app.domain.alarm.EmailAlarm;
import com.payvue.supporting.user.UserContextManager;
import com.payvue.supporting.user.command.User;
import com.payvue.supporting.user.command.UserAccountView;
import java.util.function.Consumer;

public final class VerificationResendFlow {
  private final VerificationResendContext context;

  private VerificationResendFlow(VerificationResendContext context) {
    this.context = context;
  }

  public static VerificationResendFlow start(String email) {
    return new VerificationResendFlow(new VerificationResendContext(email));
  }

  public VerificationResendFlow user(Consumer<VerificationResendFlow> step) {
    step.accept(this);
    return this;
  }

  public VerificationResendFlow auth(Consumer<VerificationResendFlow> step) {
    step.accept(this);
    return this;
  }

  public VerificationResendFlow verify(Consumer<VerificationResendFlow> step) {
    step.accept(this);
    return this;
  }

  public VerificationResendFlow token(Consumer<VerificationResendFlow> step) {
    step.accept(this);
    return this;
  }

  public VerificationResendFlow notify(Consumer<VerificationResendFlow> step) {
    step.accept(this);
    return this;
  }

  public VerificationResendFlow loadUser(UserContextManager userContextManager) {
    User user = userContextManager.findByEmail(context.email(), User.class)
        .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    context.user(user);
    return this;
  }

  public VerificationResendFlow ensureNotVerified() {
    UserAccountView view = UserAccountView.from(context.user());
    if (view.emailVerified()) {
      throw new IllegalStateException("이미 인증된 이메일입니다.");
    }
    return this;
  }

  public VerificationResendFlow generateToken(VerificationTokenService tokenService) {
    context.verificationToken(tokenService.generateToken(context.user(), VerificationPurpose.SIGNUP));
    context.verificationUrl(tokenService.generateVerificationUrl(context.verificationToken()));
    return this;
  }

  public VerificationResendFlow issue(AuthContextManager authContextManager) {
    RedirectCompletionResult completion = authContextManager.authenticate(
        Authentication.EMAIL,
        CompletionStrategyClassification.REDIRECT,
        CompletionStrategyClassification.Sequence.ISSUE,
        AuthRequest.ofEmailIssue(context.email(), VerificationPurpose.SIGNUP)
    );
    AuthResult result = completion.result();
    if (result == null || result.isFailure()) {
      throw new IllegalArgumentException(result == null ? "invalid request" : result.error().message());
    }
    context.verificationUrl(completion.redirectUrl());
    return this;
  }

  public VerificationResendFlow sendSignupEmail(AlarmContextManager alarmChannelRouter) {
    UserAccountView view = UserAccountView.from(context.user());
    EmailAlarm alarm = AlarmFactory.createVerificationEmail(
        context.user().getId().getValue().toString(),
        view.nickname(),
        "이메일 재인증",
        context.verificationUrl()
    );
    alarmChannelRouter.send(alarm);
    return this;
  }

  public VerificationResendContext end() {
    return context;
  }
}
