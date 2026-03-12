package com.payvue.app.presentation.rest.payment;

import io.swagger.v3.oas.annotations.media.Schema;

public record PaymentReserveResponse(
    @Schema(description = "결제 예약 URL", example = "https://test-m.pay.naver.com/instantPay/nfPayment/...")
    String reserve,
    VirtualAccount virtualAccount,
    ConfirmParams confirm
) {

  public static PaymentReserveResponse redirect(String reserve) {
    return new PaymentReserveResponse(reserve, null, null);
  }

  public static PaymentReserveResponse virtualAccount(VirtualAccount virtualAccount, ConfirmParams confirm) {
    return new PaymentReserveResponse(null, virtualAccount, confirm);
  }

  public record VirtualAccount(
      String bankCode,
      String accountNumber,
      String customerName,
      String dueDate,
      String amount
  ) {}

  public record ConfirmParams(
      String payId,
      String paymentId,
      String reserveId,
      String resultCode
  ) {}
}
