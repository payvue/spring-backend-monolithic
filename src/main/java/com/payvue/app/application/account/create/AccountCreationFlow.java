package com.payvue.app.application.account.create;

import com.payvue.generic.alarm.AlarmContextManager;

import com.payvue.app.domain.account.CommandType;
import com.payvue.generic.alarm.AlarmFactory;
import com.payvue.app.domain.auth.AuthContextManager;
import com.payvue.app.domain.auth.AuthRequest;
import com.payvue.app.domain.auth.Authentication;
import com.payvue.app.domain.auth.CompletionStrategyClassification;
import com.payvue.app.domain.auth.RedirectCompletionResult;
import com.payvue.app.domain.auth.VerificationPurpose;
import com.payvue.app.domain.alarm.EmailAlarm;
import com.payvue.supporting.user.UserContextManager;
import com.payvue.supporting.user.command.PasswordPrincipal;
import com.payvue.supporting.user.command.User;
import com.payvue.supporting.user.command.UserAccountView;
import com.payvue.supporting.user.command.UserPasswordGuard;
import java.time.LocalDate;
import java.util.Objects;
import java.util.function.Consumer;
import org.springframework.security.crypto.password.PasswordEncoder;

public final class AccountCreationFlow {
  private final AccountCreation context;

  private AccountCreationFlow(AccountCreation context) {
    this.context = context;
  }

  public static AccountCreationFlow start(AccountCreateCommand command) {
    return new AccountCreationFlow(new AccountCreation(
        Objects.requireNonNull(command, "command must not be null")
    ));
  }

  public AccountCreationFlow user(Consumer<AccountCreationFlow> step) {
    step.accept(this);
    return this;
  }

  public AccountCreationFlow auth(Consumer<AccountCreationFlow> step) {
    step.accept(this);
    return this;
  }

  public AccountCreationFlow notify(Consumer<AccountCreationFlow> step) {
    step.accept(this);
    return this;
  }

  public AccountCreationFlow registerUser(UserContextManager userContextManager, PasswordEncoder encoder) {
    Objects.requireNonNull(userContextManager, "userContextManager must not be null");
    Objects.requireNonNull(encoder, "encoder must not be null");
    AccountCreateCommand command = context.command();
    userContextManager.findByEmail(command.email(), User.class).ifPresent(user -> {
      throw new IllegalArgumentException("등록된 사용자가 존재합니다.");
    });

    String encrypted = encoder.encode(command.password());
    User user = User.create(
        command.username(),
        command.email(),
        command.phone(),
        command.alias(),
        command.name(),
        LocalDate.of(command.birthYear(), command.birthMonth(), command.birthDay()),
        command.sido(),
        command.sigungu(),
        command.roadAddress(),
        command.detailAddress(),
        command.postalCode(),
        encrypted,
        UserPasswordGuard.defaultPolicy(command.password())
    );

    userContextManager.save(user, CommandType.REGISTER::name);
    context.user(user);
    return this;
  }

  public AccountCreationFlow issueSignupVerification(AuthContextManager authContextManager) {
    Objects.requireNonNull(authContextManager, "authContextManager must not be null");
    User user = context.user();
    RedirectCompletionResult completion = authContextManager.authenticate(
        Authentication.EMAIL,
        CompletionStrategyClassification.REDIRECT,
        CompletionStrategyClassification.Sequence.ISSUE,
        AuthRequest.ofEmailIssue(PasswordPrincipal.from(user).email(), VerificationPurpose.SIGNUP)
    );
    if (completion.result().isFailure()) {
      throw new IllegalArgumentException(completion.result().error().message());
    }
    context.verificationIssue(completion);
    return this;
  }

  public AccountCreationFlow sendSignupEmail(AlarmContextManager alarmChannelRouter) {
    Objects.requireNonNull(alarmChannelRouter, "alarmChannelRouter must not be null");
    User user = context.user();
    UserAccountView view = UserAccountView.from(user);
    EmailAlarm alarm = AlarmFactory.createVerificationEmail(
        view.email(),
        view.nickname(),
        "회원가입",
        context.verificationUrl()
    );
    alarmChannelRouter.send(alarm);
    return this;
  }

  public AccountCreation end() {
    return context;
  }
}
