package com.payvue.app.application.auth.login;

import com.payvue.app.domain.account.CommandType;
import com.payvue.app.domain.auth.AuthContextManager;
import com.payvue.app.domain.auth.AuthRequest;
import com.payvue.supporting.user.UserContextManager;
import com.payvue.supporting.user.command.User;
import java.time.Instant;
import java.util.function.Consumer;

import com.payvue.supporting.user.command.UserAccountView;
import org.springframework.security.core.userdetails.UserDetails;

public final class BasicLogoutFlow {
  private final UserLogout context;

  private BasicLogoutFlow(UserLogout context) {
    this.context = context;
  }

  public static BasicLogoutFlow start(UserDetails userDetails, LogoutCommand command) {
    return new BasicLogoutFlow(new UserLogout(userDetails, command));
  }

  public BasicLogoutFlow auth(Consumer<BasicLogoutFlow> step) {
    step.accept(this);
    return this;
  }

  public BasicLogoutFlow user(Consumer<BasicLogoutFlow> step) {
    step.accept(this);
    return this;
  }

  public BasicLogoutFlow save(Consumer<BasicLogoutFlow> step) {
    step.accept(this);
    return this;
  }

  public BasicLogoutFlow loadUser(UserContextManager userContextManager) {
    User user = userContextManager.findByEmail(context.userDetails().getUsername(), User.class)
            .orElseThrow(() -> new IllegalArgumentException("계정 정보를 찾을 수 없습니다."));
    context.user(user);
    return this;
  }

  public BasicLogoutFlow unauthenticate(AuthContextManager authContextManager) {
    authContextManager.deauthenticate(AuthRequest.ofLogout(UserAccountView.from(context.user()).email()));

    context.user().markLoggedOut(Instant.now());

    return this;
  }

  public BasicLogoutFlow persist(UserContextManager userContextManager) {
    userContextManager.save(context.user(), CommandType.UPDATE_PROFILE::name);
    return this;
  }

  public UserLogout end() {
    return context;
  }
}
