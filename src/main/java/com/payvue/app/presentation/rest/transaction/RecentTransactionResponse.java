package com.payvue.app.presentation.rest.transaction;

import io.swagger.v3.oas.annotations.media.Schema;

record RecentTransactionResponse(
    @Schema(description = "거래 ID", example = "1")
    String id,
    @Schema(description = "거래 타입", example = "asset_purchase")
    String type,
    @Schema(description = "금액", example = "-50000")
    long amount,
    @Schema(description = "설명", example = "구글 플레이 기프트카드 구매")
    String description,
    @Schema(description = "거래 날짜", example = "2024-01-15")
    String date,
    @Schema(description = "발생 시각", example = "2024-01-15T10:00:00+09:00")
    String occurredAt,
    @Schema(description = "상태", example = "completed")
    String status,
    @Schema(description = "카테고리", example = "온라인 자산")
    String category,
    @Schema(description = "메시지", example = "기프트카드 구매 완료")
    String message
) {}
