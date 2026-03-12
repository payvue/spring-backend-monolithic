package com.payvue.app.application.auth.login;

import com.payvue.app.domain.auth.AuthSession;
import com.payvue.supporting.user.command.User;

public final class UserLogin {
  private final LoginCommand command;
  private User user;
  private AuthSession session;

  public UserLogin(LoginCommand command) {
    this.command = command;
  }

  public LoginCommand command() {
    return command;
  }

  public User user() {
    return user;
  }

  public void user(User user) {
    this.user = user;
  }

  public AuthSession session() {
    return session;
  }

  public void session(AuthSession session) {
    this.session = session;
  }
}
