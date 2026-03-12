package com.payvue.app.domain.security;

import com.payvue.supporting.user.command.User;
import com.payvue.supporting.user.command.UserAccountView;

import java.time.Duration;

public interface SessionTokenRepository {
  void save(String token, String subject, Duration ttl);
  boolean isActive(String token);
  void remove(String token);
  void delete(User user);
  void delete(UserAccountView view);
}
