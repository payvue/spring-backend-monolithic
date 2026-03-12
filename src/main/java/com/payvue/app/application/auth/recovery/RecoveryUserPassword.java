package com.payvue.app.application.auth.recovery;

import com.payvue.app.domain.auth.RedirectCompletionResult;
import com.payvue.supporting.user.command.User;

public final class RecoveryUserPassword {
  private final FindPasswordCommand command;
  private User user;
  private RedirectCompletionResult result;

  public RecoveryUserPassword(FindPasswordCommand command) {
    this.command = command;
  }

  public FindPasswordCommand command() {
    return command;
  }

  public User user() {
    return user;
  }

  public void user(User user) {
    this.user = user;
  }

  public RedirectCompletionResult result() {
    return result;
  }

  public void result(RedirectCompletionResult result) {
    this.result = result;
  }
}
