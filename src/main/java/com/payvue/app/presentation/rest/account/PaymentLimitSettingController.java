package com.payvue.app.presentation.rest.account;

import com.payvue.app.application.account.setting.PaymentLimitSettingResult;
import com.payvue.app.application.account.setting.PaymentLimitSettingUseCase;
import com.payvue.app.application.account.setting.UpdatePaymentLimitSettingCommand;
import com.payvue.app.presentation.rest.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user/settings")
@RequiredArgsConstructor
class PaymentLimitSettingController {

  private final PaymentLimitSettingUseCase paymentLimitSettingUseCase;

  @GetMapping("/payment-limits")
  public ApiResponse<PaymentLimitSettingResponse> getPaymentLimits(
      @AuthenticationPrincipal UserDetails userDetails
  ) {
    PaymentLimitSettingResult result = paymentLimitSettingUseCase.get(userDetails);
    return ApiResponse.success(new PaymentLimitSettingResponse(
        result.dailyLimit(),
        result.monthlyLimit(),
        result.enableDailyLimit(),
        result.enableMonthlyLimit(),
        result.enableNotifications(),
        result.dailyUsed(),
        result.monthlyUsed()
    ));
  }

  @PutMapping("/payment-limits")
  public ApiResponse<Void> updatePaymentLimits(
      @AuthenticationPrincipal UserDetails userDetails,
      @RequestBody PaymentLimitSettingRequest request
  ) {
    UpdatePaymentLimitSettingCommand command = new UpdatePaymentLimitSettingCommand(
        request.dailyLimit(),
        request.monthlyLimit(),
        request.enableDailyLimit(),
        request.enableMonthlyLimit(),
        request.enableNotifications()
    );
    paymentLimitSettingUseCase.update(userDetails, command);
    return ApiResponse.success();
  }
}
