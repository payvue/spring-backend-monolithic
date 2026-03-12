package com.payvue.app.application.account.query;

import com.payvue.core.community.Community;
import com.payvue.shared.Money;
import com.payvue.supporting.user.command.User;
import com.payvue.supporting.user.command.UserAccountView;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Objects;
import java.util.Optional;

public final class AccountInfo {
  private final UserDetails userDetails;
  private User user;
  private UserAccountView view;
  private Optional<Community> personalCommunity;
  private Money cash;
  private Money mileage;
  private AccountInfoResult result;

  public AccountInfo(UserDetails userDetails) {
    this.userDetails = Objects.requireNonNull(userDetails, "userDetails must not be null");
    this.personalCommunity = Optional.empty();
    this.cash = Money.won(0L);
    this.mileage = Money.won(0L);
  }

  public UserDetails userDetails() {
    return userDetails;
  }

  public User user() {
    return user;
  }

  public void user(User user) {
    this.user = Objects.requireNonNull(user, "user must not be null");
  }

  public UserAccountView view() {
    return view;
  }

  public void view(UserAccountView view) {
    this.view = Objects.requireNonNull(view, "view must not be null");
  }

  public Optional<Community> personalCommunity() {
    return personalCommunity;
  }

  public void personalCommunity(Optional<Community> personalCommunity) {
    this.personalCommunity = Objects.requireNonNull(personalCommunity, "personalCommunity must not be null");
  }

  public Money cash() {
    return cash;
  }

  public void cash(Money cash) {
    this.cash = Objects.requireNonNull(cash, "cash must not be null");
  }

  public Money mileage() {
    return mileage;
  }

  public void mileage(Money mileage) {
    this.mileage = Objects.requireNonNull(mileage, "mileage must not be null");
  }

  public AccountInfoResult result() {
    return result;
  }

  public void result(AccountInfoResult result) {
    this.result = Objects.requireNonNull(result, "result must not be null");
  }
}
