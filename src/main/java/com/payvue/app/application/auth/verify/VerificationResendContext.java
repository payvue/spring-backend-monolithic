package com.payvue.app.application.auth.verify;

import com.payvue.app.domain.auth.VerificationToken;
import com.payvue.supporting.user.command.User;

public final class VerificationResendContext {
  private final String email;
  private User user;
  private VerificationToken verificationToken;
  private String verificationUrl;

  public VerificationResendContext(String email) {
    this.email = email;
  }

  public String email() {
    return email;
  }

  public User user() {
    return user;
  }

  public void user(User user) {
    this.user = user;
  }

  public VerificationToken verificationToken() {
    return verificationToken;
  }

  public void verificationToken(VerificationToken verificationToken) {
    this.verificationToken = verificationToken;
  }

  public String verificationUrl() {
    return verificationUrl;
  }

  public void verificationUrl(String verificationUrl) {
    this.verificationUrl = verificationUrl;
  }
}
