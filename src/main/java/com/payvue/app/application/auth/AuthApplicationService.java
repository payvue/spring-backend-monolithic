package com.payvue.app.application.auth;

import com.payvue.generic.alarm.AlarmContextManager;

import com.payvue.app.application.auth.recovery.FindIdCommand;
import com.payvue.app.application.auth.recovery.FindPasswordCommand;
import com.payvue.app.application.auth.login.LoginCommand;
import com.payvue.app.application.auth.login.LoginResult;
import com.payvue.app.application.auth.login.LogoutCommand;
import com.payvue.app.application.auth.login.BasicLoginFlow;
import com.payvue.app.application.auth.login.BasicLogoutFlow;
import com.payvue.app.application.auth.recovery.RecoveryUserIdFlow;
import com.payvue.app.application.auth.recovery.RecoveryPasswordFlow;
import com.payvue.app.domain.auth.AuthContextManager;
import com.payvue.supporting.user.UserContextManager;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthApplicationService implements AuthUseCase {

  private final AuthContextManager authContextManager;
  private final UserContextManager userContextManager;
  private final AlarmContextManager alarmChannelRouter;

  @Override
  public LoginResult login(LoginCommand command) {
    var context = BasicLoginFlow.start(command)
        .user(step -> step.loadUser(userContextManager))
        .auth(step -> step.verifyPassword(authContextManager))
        .save(step -> step.persist(userContextManager))
        .end();

    return LoginResult.from(context.session());
  }

  @Override
  @Transactional
  public void logout(UserDetails userDetails, LogoutCommand command) {
    BasicLogoutFlow.start(userDetails, command)
        .user(step -> step.loadUser(userContextManager))
        .auth(step -> step.unauthenticate(authContextManager))
        .save(step -> step.persist(userContextManager))
        .end();
  }

  @Override
  public void findId(FindIdCommand command) {
    RecoveryUserIdFlow.start(command)
            .user(step -> step.findByEmail(userContextManager))
            .auth(step -> step.authenticate(authContextManager))
            .notify(step -> step.sendVerificationEmail(alarmChannelRouter))
            .end();
  }

  @Override
  public void findPassword(FindPasswordCommand command) {
    RecoveryPasswordFlow.start(command)
            .user(step -> step.findByEmail(userContextManager))
            .auth(step -> step.authenticate(authContextManager))
            .notify(step -> step.sendVerificationEmail(alarmChannelRouter))
            .end();
  }

}
