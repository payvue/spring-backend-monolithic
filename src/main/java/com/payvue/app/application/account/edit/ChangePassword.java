package com.payvue.app.application.account.edit;

import com.payvue.app.domain.auth.DirectCompletionResult;
import com.payvue.supporting.user.command.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Objects;

public final class ChangePassword {
  private final UserDetails userDetails;
  private final ChangePasswordCommand command;
  private User user;
  private DirectCompletionResult verification;

  public ChangePassword(UserDetails userDetails, ChangePasswordCommand command) {
    this.userDetails = Objects.requireNonNull(userDetails, "userDetails must not be null");
    this.command = Objects.requireNonNull(command, "command must not be null");
  }

  public UserDetails userDetails() {
    return userDetails;
  }

  public ChangePasswordCommand command() {
    return command;
  }

  public User user() {
    return user;
  }

  public void user(User user) {
    this.user = Objects.requireNonNull(user, "user must not be null");
  }

  public DirectCompletionResult verification() {
    return verification;
  }

  public void verification(DirectCompletionResult verification) {
    this.verification = Objects.requireNonNull(verification, "verification must not be null");
  }
}
