package com.payvue.app.application.account.edit;

import java.util.Objects;

public record AccountEditCommand(String name, String contact) {
  public AccountEditCommand {
    Objects.requireNonNull(name, "name must not be null");
    Objects.requireNonNull(contact, "contact must not be null");
  }
}
