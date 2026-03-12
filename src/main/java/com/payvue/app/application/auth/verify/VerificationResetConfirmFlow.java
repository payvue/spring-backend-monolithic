package com.payvue.app.application.auth.verify;

import com.payvue.app.application.account.edit.ChangePasswordFlow;
import com.payvue.app.domain.account.CommandType;
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
import com.payvue.supporting.user.command.UserId;
import com.payvue.supporting.user.command.UserPasswordGuard;
import java.util.UUID;
import java.util.function.Consumer;
import org.springframework.security.crypto.password.PasswordEncoder;

public final class VerificationResetConfirmFlow {
  private final VerificationResetConfirmContext context;

  private VerificationResetConfirmFlow(VerificationResetConfirmContext context) {
    this.context = context;
  }

  public static VerificationResetConfirmFlow start(String token, String newPassword) {
    return new VerificationResetConfirmFlow(new VerificationResetConfirmContext(token, newPassword));
  }

  public VerificationResetConfirmFlow token(Consumer<VerificationResetConfirmFlow> step) {
    step.accept(this);
    return this;
  }

  public VerificationResetConfirmFlow auth(Consumer<VerificationResetConfirmFlow> step) {
    step.accept(this);
    return this;
  }

  public VerificationResetConfirmFlow user(Consumer<VerificationResetConfirmFlow> step) {
    step.accept(this);
    return this;
  }

  public VerificationResetConfirmFlow password(Consumer<VerificationResetConfirmFlow> step) {
    step.accept(this);
    return this;
  }

  public VerificationResetConfirmFlow save(Consumer<VerificationResetConfirmFlow> step) {
    step.accept(this);
    return this;
  }

  public VerificationResetConfirmFlow verify(AuthContextManager authContextManager) {
    RedirectCompletionResult completion = authContextManager.authenticate(
        Authentication.EMAIL,
        CompletionStrategyClassification.REDIRECT,
        CompletionStrategyClassification.Sequence.VERIFIFY,
        AuthRequest.ofEmailVerify(context.token(), VerificationPurpose.RESET_PASSWORD)
    );
    AuthResult result = completion.result();
    if (result == null || result.isFailure() || result.user() == null) {
      throw new IllegalArgumentException(result == null ? "invalid token" : result.error().message());
    }
    context.accountView(result.user());
    return this;
  }

  public VerificationResetConfirmFlow loadUserById(UserContextManager userContextManager) {
    UserAccountView view = context.accountView();
    UserId userId = view == null ? null : view.userId();
    if (userId == null) {
      throw new IllegalArgumentException("유효하지 않은 인증 정보입니다.");
    }
    User user = userContextManager.findById(userId, User.class)
        .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    context.user(user);
    return this;
  }

  public VerificationResetConfirmFlow changePassword(PasswordEncoder passwordEncoder) {
    User user = context.user();
    String encodedPassword = passwordEncoder.encode(context.newPassword());
    user.changePassword(encodedPassword, UserPasswordGuard.defaultPolicy(context.newPassword()));
    return this;
  }

  public VerificationResetConfirmFlow persist(UserContextManager userContextManager) {
    userContextManager.save(context.user(), CommandType.CHANGE_PASSWORD::name);
    return this;
  }

  public VerificationResetConfirmContext end() {
    return context;
  }
}
