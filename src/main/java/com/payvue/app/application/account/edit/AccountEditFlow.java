package com.payvue.app.application.account.edit;

import com.payvue.app.domain.account.CommandType;
import com.payvue.supporting.user.UserContextManager;
import com.payvue.supporting.user.command.User;
import java.util.Objects;
import java.util.function.Consumer;
import org.springframework.security.core.userdetails.UserDetails;

public final class AccountEditFlow {
  private final AccountEditContext context;

  private AccountEditFlow(AccountEditContext context) {
    this.context = context;
  }

  public static AccountEditFlow start(UserDetails userDetails, AccountEditCommand command) {
    Objects.requireNonNull(userDetails, "userDetails must not be null");
    Objects.requireNonNull(command, "command must not be null");
    return new AccountEditFlow(new AccountEditContext(userDetails, command));
  }

  public AccountEditFlow user(Consumer<AccountEditFlow> step) {
    step.accept(this);
    return this;
  }

  public AccountEditFlow profile(Consumer<AccountEditFlow> step) {
    step.accept(this);
    return this;
  }

  public AccountEditFlow save(Consumer<AccountEditFlow> step) {
    step.accept(this);
    return this;
  }

  public AccountEditFlow loadUser(UserContextManager userContextManager) {
    Objects.requireNonNull(userContextManager, "userContextManager must not be null");
    User user = userContextManager.findByEmail(context.userDetails().getUsername(), User.class)
        .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    context.user(user);
    return this;
  }

  public AccountEditFlow updateProfile() {
    AccountEditCommand command = context.command();
    User user = context.user();
    if (!command.name().isBlank()) {
      user.changeRealName(command.name());
    }
    return this;
  }

  public AccountEditFlow persist(UserContextManager userContextManager) {
    Objects.requireNonNull(userContextManager, "userContextManager must not be null");
    userContextManager.save(context.user(), CommandType.UPDATE_PROFILE::name);
    return this;
  }

  public AccountEditContext end() {
    return context;
  }
}
