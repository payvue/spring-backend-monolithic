package com.payvue.app.application.account.create;

import java.util.Objects;

public record AccountCreateCommand(
    String alias,
    String username,
    String name,
    String residentRegistrationNo,
    int birthYear,
    int birthMonth,
    int birthDay,
    String email,
    String phone,
    String password,
    String passwordConfirm,
    String sido,
    String sigungu,
    String roadAddress,
    String postalCode,
    String detailAddress
) {
  public AccountCreateCommand {
    Objects.requireNonNull(alias, "alias must not be null");
    Objects.requireNonNull(username, "username must not be null");
    Objects.requireNonNull(name, "name must not be null");
    Objects.requireNonNull(email, "email must not be null");
    Objects.requireNonNull(phone, "phone must not be null");
    Objects.requireNonNull(password, "password must not be null");
    Objects.requireNonNull(passwordConfirm, "passwordConfirm must not be null");
    Objects.requireNonNull(sido, "sido must not be null");
    Objects.requireNonNull(sigungu, "sigungu must not be null");
    Objects.requireNonNull(roadAddress, "roadAddress must not be null");
    Objects.requireNonNull(postalCode, "postalCode must not be null");
    Objects.requireNonNull(detailAddress, "detailAddress must not be null");
  }
}
