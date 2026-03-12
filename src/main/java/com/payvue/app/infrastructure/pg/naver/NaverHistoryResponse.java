package com.payvue.app.infrastructure.pg.naver;

import java.util.List;

record NaverHistoryResponse(
    String code,
    String message,
    List<NaverHistoryEntry> entries
) implements NaverPaymentResponse {

    record NaverHistoryEntry(
            String paymentId,
            String admissionState,
            String admissionTypeCode,
            String admissionYmdt
    ) {
    }

}
