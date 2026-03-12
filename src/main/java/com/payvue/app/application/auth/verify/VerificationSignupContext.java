package com.payvue.app.application.auth.verify;

import com.payvue.app.domain.auth.VerificationToken;
import com.payvue.core.community.Community;
import com.payvue.supporting.user.command.User;
import com.payvue.supporting.user.command.UserAccountView;

public final class VerificationSignupContext {
  private final String token;
  private VerificationToken verificationToken;
  private UserAccountView accountView;
  private User user;
  private Community community;
  private boolean alreadyVerified;

  public VerificationSignupContext(String token) {
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

  public Community community() {
    return community;
  }

  public void community(Community community) {
    this.community = community;
  }

  public boolean alreadyVerified() {
    return alreadyVerified;
  }

  public void alreadyVerified(boolean alreadyVerified) {
    this.alreadyVerified = alreadyVerified;
  }
}
