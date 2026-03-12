package com.payvue.app.application.auth.verify;

import com.payvue.app.application.auth.recovery.ResetPasswordVerifyResult;
import com.payvue.app.domain.account.CommandType;
import com.payvue.app.domain.auth.AuthContextManager;
import com.payvue.app.domain.auth.AuthRequest;
import com.payvue.app.domain.auth.AuthResult;
import com.payvue.app.domain.auth.Authentication;
import com.payvue.app.domain.auth.CompletionStrategyClassification;
import com.payvue.app.domain.auth.RedirectCompletionResult;
import com.payvue.app.domain.auth.VerificationPurpose;
import com.payvue.supporting.user.UserContextManager;
import com.payvue.supporting.user.command.User;
import com.payvue.supporting.user.command.UserAccountView;
import com.payvue.supporting.user.command.UserId;
import com.payvue.supporting.user.command.UserPasswordGuard;
import java.security.SecureRandom;
import java.util.function.Consumer;
import org.springframework.security.crypto.password.PasswordEncoder;

public final class VerificationResetPasswordFlow {
  private final VerificationResetPasswordContext context;

  private VerificationResetPasswordFlow(VerificationResetPasswordContext context) {
    this.context = context;
  }

  public static VerificationResetPasswordFlow start(String token) {
    return new VerificationResetPasswordFlow(new VerificationResetPasswordContext(token));
  }

  public VerificationResetPasswordFlow token(Consumer<VerificationResetPasswordFlow> step) {
    step.accept(this);
    return this;
  }

  public VerificationResetPasswordFlow auth(Consumer<VerificationResetPasswordFlow> step) {
    step.accept(this);
    return this;
  }

  public VerificationResetPasswordFlow user(Consumer<VerificationResetPasswordFlow> step) {
    step.accept(this);
    return this;
  }

  public VerificationResetPasswordFlow password(Consumer<VerificationResetPasswordFlow> step) {
    step.accept(this);
    return this;
  }

  public VerificationResetPasswordFlow map(Consumer<VerificationResetPasswordFlow> step) {
    step.accept(this);
    return this;
  }

  public VerificationResetPasswordFlow save(Consumer<VerificationResetPasswordFlow> step) {
    step.accept(this);
    return this;
  }

  public VerificationResetPasswordFlow verify(AuthContextManager authContextManager) {
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

  public VerificationResetPasswordFlow loadUserById(UserContextManager userContextManager) {
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

  public VerificationResetPasswordFlow issueTemporaryPassword(PasswordEncoder passwordEncoder) {
    String temporaryPassword = generateTemporaryPassword();
    String encodedPassword = passwordEncoder.encode(temporaryPassword);
    User user = context.user();
    user.changePassword(encodedPassword, UserPasswordGuard.defaultPolicy(temporaryPassword));
    context.temporaryPassword(temporaryPassword);
    return this;
  }

  public VerificationResetPasswordFlow persist(UserContextManager userContextManager) {
    userContextManager.save(context.user(), CommandType.CHANGE_PASSWORD::name);
    return this;
  }

  public VerificationResetPasswordFlow toResult() {
    context.result(new ResetPasswordVerifyResult(context.temporaryPassword()));
    return this;
  }

  public ResetPasswordVerifyResult end() {
    return context.result();
  }

  private String generateTemporaryPassword() {
    String upper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    String lower = "abcdefghijklmnopqrstuvwxyz";
    String digits = "0123456789";
    String all = upper + lower + digits;
    SecureRandom random = new SecureRandom();

    StringBuilder builder = new StringBuilder(12);
    builder.append(upper.charAt(random.nextInt(upper.length())));
    builder.append(lower.charAt(random.nextInt(lower.length())));
    builder.append(digits.charAt(random.nextInt(digits.length())));
    for (int i = 3; i < 12; i++) {
      builder.append(all.charAt(random.nextInt(all.length())));
    }
    char[] chars = builder.toString().toCharArray();
    for (int i = chars.length - 1; i > 0; i--) {
      int swap = random.nextInt(i + 1);
      char tmp = chars[i];
      chars[i] = chars[swap];
      chars[swap] = tmp;
    }
    return new String(chars);
  }
}
