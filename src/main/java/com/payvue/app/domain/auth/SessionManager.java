package com.payvue.app.domain.auth;

import com.payvue.supporting.user.command.UserAccountView;

public interface SessionManager {
  String SESSION_META_KEY = "session";

  AuthSession maintain(UserAccountView user);
  void suspend(UserAccountView user);
}
