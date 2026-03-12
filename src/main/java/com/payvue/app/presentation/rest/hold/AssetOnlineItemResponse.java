package com.payvue.app.presentation.rest.hold;

import com.payvue.characteristic.Consumable;
import com.payvue.characteristic.HasBalance;
import com.payvue.core.asset.AssetItem;
import com.payvue.shared.Money;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.RoundingMode;

public record AssetOnlineItemResponse(
    @Schema(description = "자산 ID", example = "0")
    String id,
    @Schema(description = "자산 이름", example = "payvue 캐시")
    String name,
    @Schema(description = "카테고리", example = "cash")
    String category,
    @Schema(description = "자산 금액", example = "124000")
    long amount,
    @Schema(description = "잔액", example = "124000")
    Long balance,
    @Schema(description = "잔여 금액", example = "12000")
    Long remainingAmount,
    @Schema(description = "구매일", example = "2024-01-15")
    String purchaseDate,
    @Schema(description = "상태", example = "active")
    String status,
    @Schema(description = "설명", example = "payvue에서 사용할 수 있는 캐시")
    String description
) {


    public static AssetOnlineItemResponse of(AssetItem item) {
        if (item == null) {
            return null;
        }
        Long balance = null;
        long amount = 0L;
        HasBalance hasBalance = item.getCapability(HasBalance.class).orElse(null);
        if (hasBalance != null) {
            Money value = hasBalance.balance();
            if (value != null && value.getAmount() != null) {
                amount = value.getAmount().setScale(0, RoundingMode.DOWN).longValue();
                balance = amount;
            }
        } else {
            Consumable consumable = item.getCapability(Consumable.class).orElse(null);
            if (consumable != null) {
                amount = consumable.quantity();
                balance = amount;
            }
        }
        return new AssetOnlineItemResponse(
                item.getId() == null ? null : item.getId().toString(),
                item.getName(),
                item.getCategory() == null ? null : item.getCategory().getValue(),
                amount,
                balance,
                null,
                null,
                item.getStatus() == null ? null : item.getStatus().name(),
                null
        );
    }
}
