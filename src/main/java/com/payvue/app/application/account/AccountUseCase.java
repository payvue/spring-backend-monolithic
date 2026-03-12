package com.payvue.app.application.account;

import com.payvue.app.application.account.create.AccountCreateCommand;
import com.payvue.app.application.account.edit.AccountEditCommand;
import com.payvue.app.application.account.query.AccountExistCommand;
import com.payvue.app.application.account.query.AccountExistResult;
import com.payvue.app.application.account.query.AccountInfoResult;
import com.payvue.app.application.account.edit.ChangePasswordCommand;
import org.springframework.security.core.userdetails.UserDetails;

public interface AccountUseCase {
  void create(AccountCreateCommand command);

  void edit(UserDetails userDetails, AccountEditCommand command);

  void changePassword(UserDetails userDetails, ChangePasswordCommand command);

  AccountExistResult exist(AccountExistCommand command);

  AccountInfoResult info(UserDetails userDetails);
}
