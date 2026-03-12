package com.payvue.app.application.auth.login;

import com.payvue.supporting.user.command.User;
import org.springframework.security.core.userdetails.UserDetails;

public final class UserLogout {
  private final UserDetails userDetails;
  private final LogoutCommand command;
  private User user;

  public UserLogout(UserDetails userDetails, LogoutCommand command) {
    this.userDetails = userDetails;
    this.command = command;
  }

  public UserDetails userDetails() {
    return userDetails;
  }

  public LogoutCommand command() {
    return command;
  }

  public User user() {
    return user;
  }

  public void user(User user) {
    this.user = user;
  }
}
