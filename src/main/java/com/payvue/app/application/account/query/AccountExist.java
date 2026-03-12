package com.payvue.app.application.account.query;

import com.payvue.supporting.user.command.UserAccountView;

import java.util.Objects;
import java.util.Optional;

public final class AccountExist {
  private final AccountExistCommand command;
  private Optional<UserAccountView> view;
  private AccountExistResult result;

  public AccountExist(AccountExistCommand command) {
    this.command = Objects.requireNonNull(command, "command must not be null");
    this.view = Optional.empty();
  }

  public AccountExistCommand command() {
    return command;
  }

  public Optional<UserAccountView> view() {
    return view;
  }

  public void view(Optional<UserAccountView> view) {
    this.view = Objects.requireNonNull(view, "view must not be null");
  }

  public AccountExistResult result() {
    return result;
  }

  public void result(AccountExistResult result) {
    this.result = Objects.requireNonNull(result, "result must not be null");
  }
}
