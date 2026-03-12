package com.payvue.app.application.account.create;

import com.payvue.app.domain.auth.RedirectCompletionResult;
import com.payvue.supporting.user.command.User;

import java.util.Objects;

public final class AccountCreation {
  private final AccountCreateCommand command;
  private User user;
  private RedirectCompletionResult verificationIssue;

  public AccountCreation(AccountCreateCommand command) {
    this.command = Objects.requireNonNull(command, "command must not be null");
  }

  public AccountCreateCommand command() {
    return command;
  }

  public User user() {
    return user;
  }

  public void user(User user) {
    this.user = Objects.requireNonNull(user, "user must not be null");
  }

  public void verificationIssue(RedirectCompletionResult issue) {
    this.verificationIssue = Objects.requireNonNull(issue, "issue must not be null");
  }

  public String verificationUrl() {
    return Objects.requireNonNull(verificationIssue, "verificationIssue must not be null")
        .redirectUrl();
  }
}
