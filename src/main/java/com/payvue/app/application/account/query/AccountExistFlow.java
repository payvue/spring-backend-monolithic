package com.payvue.app.application.account.query;

import com.payvue.supporting.user.UserContextManager;
import com.payvue.supporting.user.command.UserAccountView;
import java.util.Objects;
import java.util.function.Consumer;

public final class AccountExistFlow {
  private final AccountExist context;

  private AccountExistFlow(AccountExist context) {
    this.context = context;
  }

  public static AccountExistFlow start(AccountExistCommand command) {
    return new AccountExistFlow(new AccountExist(
        Objects.requireNonNull(command, "command must not be null")
    ));
  }

  public AccountExistFlow query(Consumer<AccountExistFlow> step) {
    step.accept(this);
    return this;
  }

  public AccountExistFlow map(Consumer<AccountExistFlow> step) {
    step.accept(this);
    return this;
  }

  public AccountExistFlow findByEmail(UserContextManager userContextManager) {
    Objects.requireNonNull(userContextManager, "userContextManager must not be null");
    context.view(
        userContextManager.findByEmail(context.command().email(), UserAccountView.class)
    );
    return this;
  }

  public AccountExistFlow toResult() {
    if (context.view().isEmpty()) {
      context.result(new AccountExistResult(null, null));
      return this;
    }
    UserAccountView view = context.view().orElseThrow();
    context.result(new AccountExistResult(
        view.displayName(),
        new AccountExistResult.Contact(view.email(), view.phoneNumber())
    ));
    return this;
  }

  public AccountExistResult end() {
    return context.result();
  }
}
