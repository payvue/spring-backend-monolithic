package com.payvue.app.infrastructure.pg.naver;

public record NaverReserveRequest(
    String merchantPayKey,
    String merchantUserKey,
    String productName,
    int productCount,
    long totalPayAmount,
    long taxScopeAmount,
    long taxExScopeAmount,
    String returnUrl
) {
}
