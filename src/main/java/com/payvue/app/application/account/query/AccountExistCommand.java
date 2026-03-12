package com.payvue.app.application.account.query;

import java.util.Objects;

public record AccountExistCommand(String email) {
  public AccountExistCommand {
    Objects.requireNonNull(email, "email must not be null");
  }
}
