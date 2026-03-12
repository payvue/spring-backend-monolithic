package com.payvue.app.application.account.edit;

import java.util.Objects;

public record ChangePasswordCommand(String currentPassword, String newPassword) {
  public ChangePasswordCommand {
    Objects.requireNonNull(currentPassword, "currentPassword must not be null");
    Objects.requireNonNull(newPassword, "newPassword must not be null");
  }
}
