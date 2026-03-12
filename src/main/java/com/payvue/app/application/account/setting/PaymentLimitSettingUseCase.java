package com.payvue.app.application.account.setting;

import org.springframework.security.core.userdetails.UserDetails;

public interface PaymentLimitSettingUseCase {

  PaymentLimitSettingResult get(UserDetails userDetails);

  void update(UserDetails userDetails, UpdatePaymentLimitSettingCommand command);
}
