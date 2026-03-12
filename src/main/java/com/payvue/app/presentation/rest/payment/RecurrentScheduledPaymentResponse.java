package com.payvue.app.presentation.rest.payment;

import io.swagger.v3.oas.annotations.media.Schema;

public record RecurrentScheduledPaymentResponse(
    @Schema(description = "정기 결제 ID", example = "1")
    String id,
    @Schema(description = "결제명", example = "넷플릭스 프리미엄")
    String name,
    @Schema(description = "가맹점명", example = "넷플릭스 프리미엄")
    String merchantName,
    @Schema(description = "결제 금액", example = "17000")
    long amount,
    @Schema(description = "결제 수단", example = "PayEasy 잔액")
    String paymentMethod,
    @Schema(description = "상태", example = "active")
    String status,
    @Schema(description = "마지막 결제일", example = "2024-01-15")
    String lastPaymentDate,
    @Schema(description = "다음 결제일", example = "2024-02-15")
    String nextPaymentDate,
    @Schema(description = "실패 사유", example = "잔액 부족")
    String failureReason,
    @Schema(description = "스케줄 전략")
    Strategy strategy,
    @Schema(description = "활성화 여부", example = "true")
    boolean isActive,
    @Schema(description = "다음 트리거", example = "다음 결제: 2024-02-15")
    String nextTrigger,
    @Schema(description = "마지막 트리거", example = "2024-01-15")
    String lastTriggered
) {

  public record Strategy(
      @Schema(description = "전략 타입", example = "schedule")
      String type,
      @Schema(description = "주기", example = "monthly")
      String scheduleType
  ) {}
}
