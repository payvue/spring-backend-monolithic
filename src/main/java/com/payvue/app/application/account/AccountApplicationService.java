package com.payvue.app.application.account;

import com.payvue.generic.alarm.AlarmContextManager;

import com.payvue.app.application.account.create.AccountCreation;
import com.payvue.app.application.account.create.AccountCreationFlow;
import com.payvue.app.application.account.edit.AccountEditFlow;
import com.payvue.app.application.account.edit.ChangePasswordFlow;
import com.payvue.app.application.account.query.AccountExistFlow;
import com.payvue.app.application.account.query.AccountInfoFlow;
import com.payvue.app.application.account.create.AccountCreateCommand;
import com.payvue.app.application.account.edit.AccountEditCommand;
import com.payvue.app.application.account.query.AccountExistCommand;
import com.payvue.app.application.account.query.AccountExistResult;
import com.payvue.app.application.account.query.AccountInfoResult;
import com.payvue.app.application.account.edit.ChangePasswordCommand;
import com.payvue.app.domain.asset.OnlineCashManager;
import com.payvue.app.domain.auth.AuthContextManager;
import com.payvue.core.community.CommunityContextManager;
import com.payvue.supporting.kyc.UserKyc;
import com.payvue.supporting.kyc.UserKycRepository;
import com.payvue.supporting.user.UserContextManager;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AccountApplicationService implements AccountUseCase {

  private final AuthContextManager authContextManager;
  private final UserContextManager userContextManager;
  private final CommunityContextManager communityContextManager;
  private final OnlineCashManager cashManager;

  private final PasswordEncoder passwordEncoder;
  private final AlarmContextManager alarmContextManager;

  private final UserKycRepository userKycRepository;

  @Override
  @Transactional
  public void create(AccountCreateCommand command) {
    Objects.requireNonNull(command, "command must not be null");
    AccountCreation context =
        AccountCreationFlow.start(command)
            .user(step -> step.registerUser(userContextManager, passwordEncoder))
            .auth(step -> step.issueSignupVerification(authContextManager))
            .notify(step -> step.sendSignupEmail(alarmContextManager))
            .end();

    String residentRegistrationNo = command.residentRegistrationNo();
    if (residentRegistrationNo != null && !residentRegistrationNo.isBlank()) {
      userKycRepository.save(UserKyc.pending(context.user().getId(), residentRegistrationNo));
    } else {
      userKycRepository.deleteByUserId(context.user().getId());
    }
  }

  @Override
  public void edit(UserDetails userDetails, AccountEditCommand command) {
    Objects.requireNonNull(userDetails, "userDetails must not be null");
    Objects.requireNonNull(command, "command must not be null");
    AccountEditFlow.start(userDetails, command)
        .user(step -> step.loadUser(userContextManager))
        .profile(AccountEditFlow::updateProfile)
        .save(step -> step.persist(userContextManager))
        .end();
  }

  @Override
  public void changePassword(UserDetails userDetails, ChangePasswordCommand command) {
    Objects.requireNonNull(userDetails, "userDetails must not be null");
    Objects.requireNonNull(command, "command must not be null");
    ChangePasswordFlow.start(userDetails, command)
        .user(step -> step.changePassword(userContextManager, passwordEncoder))
        .auth(step -> step.unauthenticate(authContextManager))
        .save(step -> step.persist(userContextManager))
        .end();
  }

  @Override
  public AccountExistResult exist(AccountExistCommand command) {
    Objects.requireNonNull(command, "command must not be null");
    return AccountExistFlow.start(command)
        .query(step -> step.findByEmail(userContextManager))
        .map(AccountExistFlow::toResult)
        .end();
  }

  @Override
  public AccountInfoResult info(UserDetails userDetails) {
    Objects.requireNonNull(userDetails, "userDetails must not be null");
    return AccountInfoFlow.start(userDetails)
        .user(step -> step.loadUser(userContextManager))
        .community(step -> step.loadPersonalCommunity(communityContextManager))
        .asset(step -> step.loadAssets(cashManager))
        .map(AccountInfoFlow::toResult)
        .end();
  }


}
