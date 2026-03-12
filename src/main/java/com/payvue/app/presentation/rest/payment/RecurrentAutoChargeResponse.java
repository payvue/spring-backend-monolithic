package com.payvue.app.presentation.rest.payment;

import io.swagger.v3.oas.annotations.media.Schema;

public record RecurrentAutoChargeResponse(
    @Schema(description = "자동 충전 전략 ID", example = "auto-001")
    String id,
    @Schema(description = "전략 이름", example = "잔액 1만원 이하 자동 충전")
    String name,
    @Schema(description = "결제 수단", example = "신한카드")
    String paymentMethod,
    @Schema(description = "활성화 여부", example = "true")
    boolean isActive,
    @Schema(description = "충전 금액", example = "30000")
    long chargeAmount,
    @Schema(description = "전략 조건")
    Strategy strategy
) {

  public record Strategy(
      @Schema(description = "전략 타입", example = "condition")
      String type,
      @Schema(description = "트리거 타입", example = "balance_below")
      String triggerType,
      @Schema(description = "트리거 값", example = "10000")
      int triggerValue,
      @Schema(description = "설명", example = "잔액 부족 시 자동 충전")
      String description
  ) {}
}
