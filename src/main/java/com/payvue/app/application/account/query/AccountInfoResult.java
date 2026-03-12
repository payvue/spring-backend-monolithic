package com.payvue.app.application.account.query;

public record AccountInfoResult(
    Account account,
    Info info,
    Asset asset
) {

  public record Account(String alias, String id, Status status) {}

  public record Status(String lastLoggedIn, String joinAt) {}

  public record Info(String name, Contact contact, String avatar) {}

  public record Contact(String email, String phone) {}

  public record Asset(long balance, long point) {}
}
