package com.payvue.app.application.auth.recovery;

import com.payvue.generic.alarm.AlarmContextManager;

import com.payvue.generic.alarm.AlarmFactory;
import com.payvue.app.domain.auth.*;
import com.payvue.app.domain.alarm.EmailAlarm;
import com.payvue.supporting.user.UserContextManager;
import com.payvue.supporting.user.command.User;
import com.payvue.supporting.user.command.UserAccountView;

import java.util.function.Consumer;

public final class RecoveryUserIdFlow {
  private final RecoveryUserId context;

  private RecoveryUserIdFlow(RecoveryUserId context) {
    this.context = context;
  }

  public static RecoveryUserIdFlow start(FindIdCommand command) {
    return new RecoveryUserIdFlow(new RecoveryUserId(command));
  }

  public RecoveryUserIdFlow user(Consumer<RecoveryUserIdFlow> step) {
    step.accept(this);
    return this;
  }

  public RecoveryUserIdFlow auth(Consumer<RecoveryUserIdFlow> step) {
    step.accept(this);
    return this;
  }

  public RecoveryUserIdFlow notify(Consumer<RecoveryUserIdFlow> step) {
    step.accept(this);
    return this;
  }

  public RecoveryUserIdFlow findByEmail(UserContextManager userContextManager) {
    User user = userContextManager.findByEmail(
        context.command().email(),
        User.class
    )
        .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    context.user(user);
    return this;
  }

  public RecoveryUserIdFlow authenticate(AuthContextManager authContextManager) {

    UserAccountView view = UserAccountView.from(context.user());

    RedirectCompletionResult result = authContextManager.authenticate(Authentication.EMAIL,
            CompletionStrategyClassification.REDIRECT,
            CompletionStrategyClassification.Sequence.ISSUE,
            AuthRequest.ofEmailIssue(view.email(), VerificationPurpose.FIND_ID));


    context.result(result);
    return this;
  }

  public RecoveryUserIdFlow sendVerificationEmail(AlarmContextManager alarmChannelRouter) {

    UserAccountView view = UserAccountView.from(context.user());

    EmailAlarm alarm = AlarmFactory.createVerificationEmail(
            view.email(),
            view.nickname(),
            "아이디 찾기",
            context.result().redirectUrl()
    );
    alarmChannelRouter.send(alarm);
    return this;
  }


  public RecoveryUserId end() {
    return context;
  }
}
