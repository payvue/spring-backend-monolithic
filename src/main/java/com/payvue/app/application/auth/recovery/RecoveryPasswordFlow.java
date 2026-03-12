package com.payvue.app.application.auth.recovery;

import com.payvue.generic.alarm.AlarmContextManager;

import com.payvue.generic.alarm.AlarmFactory;
import com.payvue.app.domain.auth.*;
import com.payvue.app.domain.alarm.EmailAlarm;
import com.payvue.supporting.user.UserContextManager;
import com.payvue.supporting.user.command.User;
import com.payvue.supporting.user.command.UserAccountView;

import java.util.function.Consumer;

public final class RecoveryPasswordFlow {
  private final RecoveryUserPassword context;

  private RecoveryPasswordFlow(RecoveryUserPassword context) {
    this.context = context;
  }

  public static RecoveryPasswordFlow start(FindPasswordCommand command) {
    return new RecoveryPasswordFlow(new RecoveryUserPassword(command));
  }

  public RecoveryPasswordFlow user(Consumer<RecoveryPasswordFlow> step) {
    step.accept(this);
    return this;
  }

  public RecoveryPasswordFlow auth(Consumer<RecoveryPasswordFlow> step) {
    step.accept(this);
    return this;
  }

  public RecoveryPasswordFlow verify(Consumer<RecoveryPasswordFlow> step) {
    step.accept(this);
    return this;
  }

  public RecoveryPasswordFlow notify(Consumer<RecoveryPasswordFlow> step) {
    step.accept(this);
    return this;
  }

  public RecoveryPasswordFlow findByEmail(UserContextManager userContextManager) {
    User user = userContextManager.findByEmail(context.command().email(), User.class)
        .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    context.user(user);
    return this;
  }

  public RecoveryPasswordFlow authenticate(AuthContextManager authContextManager) {

    UserAccountView view = UserAccountView.from(context.user());

    RedirectCompletionResult result = authContextManager.authenticate(Authentication.EMAIL,
            CompletionStrategyClassification.REDIRECT,
            CompletionStrategyClassification.Sequence.ISSUE,
            AuthRequest.ofEmailIssue(view.email(), VerificationPurpose.RESET_PASSWORD));

    context.result(result);
    return this;
  }

  public RecoveryPasswordFlow sendVerificationEmail(AlarmContextManager alarmChannelRouter) {

    UserAccountView view = UserAccountView.from(context.user());

    EmailAlarm alarm = AlarmFactory.createVerificationEmail(
            view.email(),
            view.nickname(),
            "비밀번호 재설정",
            context.result().redirectUrl()

    );
    alarmChannelRouter.send(alarm);
    return this;
  }

  public RecoveryUserPassword end() {
    return context;
  }
}
