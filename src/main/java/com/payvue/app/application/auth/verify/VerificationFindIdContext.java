package com.payvue.app.application.auth.verify;

import com.payvue.app.application.auth.recovery.FindIdResult;
import com.payvue.app.domain.auth.VerificationToken;
import com.payvue.supporting.user.command.User;
import com.payvue.supporting.user.command.UserAccountView;

public final class VerificationFindIdContext {
  private final String token;
  private VerificationToken verificationToken;
  private UserAccountView accountView;
  private User user;
  private FindIdResult result;

  public VerificationFindIdContext(String token) {
    this.token = token;
  }

  public String token() {
    return token;
  }

  public VerificationToken verificationToken() {
    return verificationToken;
  }

  public void verificationToken(VerificationToken verificationToken) {
    this.verificationToken = verificationToken;
  }

  public UserAccountView accountView() {
    return accountView;
  }

  public void accountView(UserAccountView accountView) {
    this.accountView = accountView;
  }

  public User user() {
    return user;
  }

  public void user(User user) {
    this.user = user;
  }

  public FindIdResult result() {
    return result;
  }

  public void result(FindIdResult result) {
    this.result = result;
  }
}
