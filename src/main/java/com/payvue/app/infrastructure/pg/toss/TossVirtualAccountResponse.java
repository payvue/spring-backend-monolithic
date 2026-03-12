package com.payvue.app.infrastructure.pg.toss;

import com.payvue.generic.payment.PaymentProviderResponse;

record TossVirtualAccountResponse(
    String status,
    String paymentKey,
    String orderId,
    Integer amount,
    VirtualAccount virtualAccount
) implements PaymentProviderResponse {

  record VirtualAccount(
      String accountNumber,
      String bankCode,
      String customerName,
      String dueDate
  ) {}
}
