package com.payvue.app.infrastructure.pg.naver;

record NaverApplyResponse(
    String code,
    String message,
    String paymentId,
    String admissionState
) implements NaverPaymentResponse {
}
