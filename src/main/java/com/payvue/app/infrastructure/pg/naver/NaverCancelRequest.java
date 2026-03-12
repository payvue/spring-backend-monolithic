package com.payvue.app.infrastructure.pg.naver;

record NaverCancelRequest(
    String paymentId,
    long cancelAmount,
    String cancelReason,
    String cancelRequester,
    long taxScopeAmount,
    long taxExScopeAmount
) {
}
