package com.payvue.app.application.account.edit;

import com.payvue.supporting.user.command.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Objects;

public final class AccountEditContext {
  private final UserDetails userDetails;
  private final AccountEditCommand command;
  private User user;

  public AccountEditContext(UserDetails userDetails, AccountEditCommand command) {
    this.userDetails = Objects.requireNonNull(userDetails, "userDetails must not be null");
    this.command = Objects.requireNonNull(command, "command must not be null");
  }

  public UserDetails userDetails() {
    return userDetails;
  }

  public AccountEditCommand command() {
    return command;
  }

  public User user() {
    return user;
  }

  public void user(User user) {
    this.user = Objects.requireNonNull(user, "user must not be null");
  }
}
