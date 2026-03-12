package com.payvue.supporting.user.command;

import com.payvue.supporting.user.CommandPolicy;
import com.payvue.supporting.user.UserCommand;

import java.util.function.Function;

public class PasswordPolicySpec implements CommandPolicy {

  private final Function<String, PasswordPolicy> policyProvider;

  public PasswordPolicySpec(Function<String, PasswordPolicy> policyProvider) {
    if (policyProvider == null) {
      throw new IllegalArgumentException("policyProvider is required");
    }
    this.policyProvider = policyProvider;
  }

  @Override
  public boolean validate(UserCommand command) {
    if (!(command instanceof PasswordPolicyCommand passwordCommand)) {
      return true;
    }
    String raw = passwordCommand.rawPassword();
    String encrypted = passwordCommand.encryptedPassword();
    if (raw == null || raw.isBlank() || encrypted == null || encrypted.isBlank()) {
      return false;
    }
    PasswordPolicy policy = policyProvider.apply(raw);
    if (policy == null) {
      return false;
    }
    PasswordHistory history = passwordCommand.history();
    policy.validate(encrypted, history == null ? PasswordHistory.empty() : history);
    return true;
  }
}
