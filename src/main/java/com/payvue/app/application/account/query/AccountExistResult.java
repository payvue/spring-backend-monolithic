package com.payvue.app.application.account.query;

public record AccountExistResult(String name, Contact contact) {

  public record Contact(String email, String phone) {}
}
