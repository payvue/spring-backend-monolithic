package com.payvue.app.application.auth;

import com.payvue.app.application.auth.login.LoginCommand;
import com.payvue.app.application.auth.login.LoginResult;
import com.payvue.app.application.auth.login.LogoutCommand;
import com.payvue.app.application.auth.recovery.FindIdCommand;
import com.payvue.app.application.auth.recovery.FindPasswordCommand;
import org.springframework.security.core.userdetails.UserDetails;

public interface AuthUseCase {
  LoginResult login(LoginCommand command);

  void logout(UserDetails user, LogoutCommand command);

  void findId(FindIdCommand command);

  void findPassword(FindPasswordCommand command);
}
