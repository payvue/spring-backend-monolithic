package com.payvue.app.application.account.edit;

import com.payvue.app.domain.account.CommandType;
import com.payvue.app.domain.auth.AuthContextManager;
import com.payvue.app.domain.auth.AuthRequest;
import com.payvue.supporting.user.UserContextManager;
import com.payvue.supporting.user.command.User;
import com.payvue.supporting.user.command.UserAccountView;
import com.payvue.supporting.user.command.UserPasswordGuard;
import java.util.Objects;
import java.util.function.Consumer;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

public final class ChangePasswordFlow {
  private final ChangePassword context;

  private ChangePasswordFlow(ChangePassword context) {
    this.context = context;
  }

  public static ChangePasswordFlow start(UserDetails userDetails, ChangePasswordCommand command) {
    Objects.requireNonNull(userDetails, "userDetails must not be null");
    Objects.requireNonNull(command, "command must not be null");
    return new ChangePasswordFlow(new ChangePassword(userDetails, command));
  }

  public ChangePasswordFlow user(Consumer<ChangePasswordFlow> step) {
    step.accept(this);
    return this;
  }

  public ChangePasswordFlow auth(Consumer<ChangePasswordFlow> step) {
    step.accept(this);
    return this;
  }

  public ChangePasswordFlow password(Consumer<ChangePasswordFlow> step) {
    step.accept(this);
    return this;
  }

  public ChangePasswordFlow save(Consumer<ChangePasswordFlow> step) {
    step.accept(this);
    return this;
  }

  public ChangePasswordFlow loadUser(UserContextManager userContextManager) {
    Objects.requireNonNull(userContextManager, "userContextManager must not be null");
    User user = userContextManager.findByEmail(context.userDetails().getUsername(), User.class)
        .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    context.user(user);
    return this;
  }

  public ChangePasswordFlow unauthenticate(AuthContextManager authContextManager) {
    Objects.requireNonNull(authContextManager, "authContextManager must not be null");
    authContextManager.deauthenticate(AuthRequest.ofLogout(UserAccountView.from(context.user()).email()));
    return this;
  }

  public ChangePasswordFlow changePassword(UserContextManager userContextManager, PasswordEncoder encoder) {
    Objects.requireNonNull(encoder, "encoder must not be null");

    User user = userContextManager.findByEmail(context.userDetails().getUsername(), User.class)
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

    ChangePasswordCommand command = context.command();
    String encoded = encoder.encode(command.newPassword());
    user.changePassword(encoded, UserPasswordGuard.defaultPolicy(command.newPassword()));

    context.user(user);
    return this;
  }

  public ChangePasswordFlow persist(UserContextManager userContextManager) {
    Objects.requireNonNull(userContextManager, "userContextManager must not be null");
    userContextManager.save(context.user(), CommandType.CHANGE_PASSWORD::name);
    return this;
  }

  public ChangePassword end() {
    return context;
  }
}
