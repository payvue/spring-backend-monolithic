package com.payvue.app.infrastructure.pg.naver;

record NaverCancelResponse(
    String code,
    String message,
    String paymentId
) implements NaverPaymentResponse {
}
