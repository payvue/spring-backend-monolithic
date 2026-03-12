package com.payvue.app.application.auth.login;

import com.payvue.app.domain.account.CommandType;
import com.payvue.app.domain.auth.AuthContextManager;
import com.payvue.app.domain.auth.AuthRequest;
import com.payvue.app.domain.auth.AuthResult;
import com.payvue.app.domain.auth.Authentication;
import com.payvue.app.domain.auth.CompletionStrategyClassification;
import com.payvue.app.domain.auth.DirectCompletionResult;
import com.payvue.app.domain.auth.AuthSession;
import com.payvue.app.domain.auth.SessionManager;
import com.payvue.supporting.user.UserContextManager;
import com.payvue.supporting.user.command.PasswordPrincipal;
import com.payvue.supporting.user.command.User;

import java.time.Instant;
import java.util.function.Consumer;

public final class BasicLoginFlow {
  private final UserLogin context;

  private BasicLoginFlow(UserLogin context) {
    this.context = context;
  }

  public static BasicLoginFlow start(LoginCommand command) {
    return new BasicLoginFlow(new UserLogin(command));
  }

  public BasicLoginFlow user(Consumer<BasicLoginFlow> step) {
    step.accept(this);
    return this;
  }

  public BasicLoginFlow auth(Consumer<BasicLoginFlow> step) {
    step.accept(this);
    return this;
  }

  public BasicLoginFlow save(Consumer<BasicLoginFlow> step) {
    step.accept(this);
    return this;
  }

  public BasicLoginFlow loadUser(UserContextManager userContextManager) {
    User user = userContextManager.findByEmail(context.command().email(), User.class)
        .orElseThrow(() -> new IllegalArgumentException("계정 정보를 찾을 수 없습니다."));
    context.user(user);
    return this;
  }

  public BasicLoginFlow verifyPassword(AuthContextManager authContextManager) {
    User user = context.user();
    DirectCompletionResult completion = authContextManager.authenticate(
        Authentication.PASSWORD,
        CompletionStrategyClassification.DIRECT,
        CompletionStrategyClassification.Sequence.VERIFIFY,
        AuthRequest.ofPassword(PasswordPrincipal.from(user).email(), context.command().password())
    );
    AuthResult result = completion.result();
    if (result.isFailure()) {
      throw new IllegalArgumentException(result.error().message());
    }
    Object value = result.meta().get(SessionManager.SESSION_META_KEY);
    if (value instanceof AuthSession session) {
      context.session(session);
   }

    context.user().markLoggedIn(Instant.now());

    return this;
  }

  public BasicLoginFlow persist(UserContextManager userContextManager) {
    userContextManager.save(context.user(), CommandType.UPDATE_PROFILE::name);
    return this;
  }

  public UserLogin end() {
    return context;
  }


}
