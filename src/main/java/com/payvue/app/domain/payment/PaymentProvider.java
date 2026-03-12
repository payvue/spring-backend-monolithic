package com.payvue.app.domain.payment;

public enum PaymentProvider {
  KAKAO,
  NAVER,
  TOSS;

  public static PaymentProvider from(String raw) {
    if (raw == null || raw.isBlank()) {
      throw new IllegalArgumentException("payment provider is required");
    }
    return valueOf(raw.trim().toUpperCase());
  }
}
