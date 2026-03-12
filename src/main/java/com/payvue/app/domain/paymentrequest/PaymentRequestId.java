package com.payvue.app.domain.paymentrequest;

import java.util.UUID;

public final class PaymentRequestId {

  private final UUID value;

  private PaymentRequestId(UUID value) {
    if (value == null) {
      throw new IllegalArgumentException("paymentRequestId는 필수입니다.");
    }
    this.value = value;
  }

  public static PaymentRequestId of(UUID value) {
    return new PaymentRequestId(value);
  }

  public static PaymentRequestId generate() {
    return new PaymentRequestId(UUID.randomUUID());
  }

  public UUID value() {
    return value;
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
