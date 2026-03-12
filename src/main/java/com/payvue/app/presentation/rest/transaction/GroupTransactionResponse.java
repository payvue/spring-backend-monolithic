package com.payvue.app.presentation.rest.transaction;

import io.swagger.v3.oas.annotations.media.Schema;

record GroupTransactionResponse(
    @Schema(description = "거래 ID", example = "1")
    String id,
    @Schema(description = "거래 타입", example = "payment")
    String type,
    @Schema(description = "금액", example = "-45000")
    long amount,
    @Schema(description = "설명", example = "가족 저녁 식사 - 한정식집")
    String description,
    @Schema(description = "결제자", example = "김아빠")
    String paidBy,
    @Schema(description = "거래 날짜", example = "2024-01-15")
    String date,
    @Schema(description = "상태", example = "completed")
    String status,
    @Schema(description = "카테고리", example = "식비")
    String category
) {}
