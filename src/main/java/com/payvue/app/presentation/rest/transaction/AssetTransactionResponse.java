package com.payvue.app.presentation.rest.transaction;

import io.swagger.v3.oas.annotations.media.Schema;

record AssetTransactionResponse(
    @Schema(description = "거래 ID", example = "1")
    String id,
    @Schema(description = "거래 타입", example = "purchase")
    String type,
    @Schema(description = "자산 이름", example = "구글 플레이 기프트카드")
    String assetName,
    @Schema(description = "금액", example = "50000")
    long amount,
    @Schema(description = "거래 일시", example = "2024-01-14T09:10:00+09:00")
    String date,
    @Schema(description = "상태", example = "completed")
    String status,
    @Schema(description = "카테고리", example = "기프트카드")
    String category,
    @Schema(description = "그룹 이름", example = "가족 계정")
    String groupName,
    @Schema(description = "관련 사용자", example = "김형")
    String relatedUser
) {}
