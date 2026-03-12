package com.payvue.app.infrastructure.pg.toss;

import com.payvue.generic.payment.*;
import com.payvue.app.domain.payment.PaymentProvider;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class TossPaymentTranslator implements PaymentTranslator<PaymentProviderResponse> {

  @Override
  public ProviderId providerId() {
    return new ProviderId(PaymentProvider.TOSS.name());
  }

  @Override
  public Payment translate(PaymentProviderResponse response) {
    if (response == null) {
      return null;
    }
    if (response instanceof TossVirtualAccountResponse va) {
      PaymentStatus status = mapStatus(va.status());
      String accountNumber = va.virtualAccount() != null ? va.virtualAccount().accountNumber() : null;
      String bankCode = va.virtualAccount() != null ? va.virtualAccount().bankCode() : null;
      String customerName = va.virtualAccount() != null ? va.virtualAccount().customerName() : null;
      String dueDate = va.virtualAccount() != null ? va.virtualAccount().dueDate() : null;
      Map<String, String> metadata = new HashMap<>();
      if (va.paymentKey() != null) metadata.put("paymentKey", va.paymentKey());
      if (va.orderId() != null) metadata.put("orderId", va.orderId());
      if (bankCode != null) metadata.put("bankCode", bankCode);
      if (accountNumber != null) metadata.put("accountNumber", accountNumber);
      if (customerName != null) metadata.put("customerName", customerName);
      if (dueDate != null) metadata.put("dueDate", dueDate);
      if (va.amount() != null) metadata.put("amount", va.amount().toString());
      if (va.status() != null) metadata.put("status", va.status());

      return Payment.of(
          status,
          PaymentModeType.VIRTUAL_ACCOUNT,
          providerId(),
          va.paymentKey(),
          metadata
      );
    }
    if (response instanceof VirtualAccountTossGateway.TossPaymentResponse toss) {
      PaymentStatus status = mapStatus(toss.status());
      Map<String, String> metadata = new HashMap<>();
      if (toss.paymentKey() != null) metadata.put("paymentKey", toss.paymentKey());
      if (toss.orderId() != null) metadata.put("orderId", toss.orderId());
      if (toss.totalAmount() != null) metadata.put("totalAmount", toss.totalAmount().toString());
      if (toss.method() != null) metadata.put("method", toss.method());
      if (toss.status() != null) metadata.put("status", toss.status());

      return Payment.of(
          status,
          PaymentModeType.VIRTUAL_ACCOUNT,
          providerId(),
          toss.paymentKey(),
          metadata
      );
    }
    return Payment.of(
        PaymentStatus.FAILED,
        PaymentModeType.VIRTUAL_ACCOUNT,
        providerId(),
        null,
        Map.of("message", "지원하지 않는 Toss 응답 형식")
    );
  }

  private PaymentStatus mapStatus(String status) {
    if (status == null) {
      return PaymentStatus.FAILED;
    }
    return switch (status.toUpperCase()) {
      case "READY", "IN_PROGRESS", "WAITING_FOR_DEPOSIT" -> PaymentStatus.PENDING;
      case "DONE" -> PaymentStatus.APPROVED;
      case "PARTIAL_CANCELED", "CANCELED" -> PaymentStatus.CANCELED;
      default -> PaymentStatus.FAILED;
    };
  }
}
