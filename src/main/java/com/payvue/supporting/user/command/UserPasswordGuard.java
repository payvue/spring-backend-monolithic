package com.payvue.supporting.user.command;


public final class UserPasswordGuard {

  public static void validate(User user, String raw, String encrypted) {
    Credentials credentials = user == null ? null : user.getCredentials();
    PasswordHistory history = credentials == null ? PasswordHistory.empty() : credentials.getHistory();
    DefaultPasswordPolicy.defaults(raw).validate(encrypted, history);
  }

  public static void validateNew(String raw, String encrypted) {
    DefaultPasswordPolicy.defaults(raw).validate(encrypted, PasswordHistory.empty());
  }

  public static void ensureNotReused(User user, String encodedPassword) {
    if (user == null || encodedPassword == null) {
      return;
    }
    Credentials credentials = user.getCredentials();
    if (credentials == null) {
      return;
    }
    if (encodedPassword.equals(credentials.getCurrent())) {
      throw new IllegalArgumentException("이전에 사용한 비밀번호는 사용할 수 없습니다.");
    }
    PasswordHistory history = credentials.getHistory();
    if (history != null && history.contains(encodedPassword)) {
      throw new IllegalArgumentException("이전에 사용한 비밀번호는 사용할 수 없습니다.");
    }
  }

  public static PasswordPolicy defaultPolicy(String password) {
    return DefaultPasswordPolicy.defaults(password);
  }
}
