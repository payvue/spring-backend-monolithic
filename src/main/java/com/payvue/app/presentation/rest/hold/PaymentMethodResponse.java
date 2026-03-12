package com.payvue.app.presentation.rest.hold;

import io.swagger.v3.oas.annotations.media.Schema;

public record PaymentMethodResponse(
    @Schema(description = "결제 수단 ID", example = "pm-001")
    String id,
    @Schema(description = "결제 수단 타입", example = "card")
    String type,
    @Schema(description = "결제 수단 이름", example = "결제 카드")
    String name,
    @Schema(description = "결제 수단 제공사", example = "신한카드")
    String provider,
    @Schema(description = "기본 결제 수단 여부", example = "true")
    boolean isDefault,
    @Schema(description = "추가 설명", example = "잔액 35,000원")
    String details
) {}
