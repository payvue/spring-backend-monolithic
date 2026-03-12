package com.payvue.app.application.auth.verify;

import com.payvue.app.domain.auth.VerificationToken;
import com.payvue.supporting.user.command.User;
import com.payvue.supporting.user.command.UserAccountView;

public final class VerificationResetConfirmContext {
  private final String token;
  private final String newPassword;
  private VerificationToken verificationToken;
  private UserAccountView accountView;
  private User user;

  public VerificationResetConfirmContext(String token, String newPassword) {
    this.token = token;
    this.newPassword = newPassword;
  }

  public String token() {
    return token;
  }

  public String newPassword() {
    return newPassword;
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
}
