package com.payvue.app.domain.asset.operation;

import com.payvue.characteristic.Consumable;
import com.payvue.core.asset.AssetItem;
import com.payvue.core.asset.characteristic.CapabilityManager;
import com.payvue.core.asset.characteristic.CapabilityResult;
import com.payvue.core.asset.characteristic.command.QuantityCommand;
import com.payvue.core.hold.AssetOperationUnit;
import com.payvue.core.hold.Holdings;
import com.payvue.core.hold.OperationResult;
import com.payvue.core.hold.OperationType;

import java.util.Optional;

public class DisposeConsumableOperation implements AssetOperationUnit {

    private final Holdings holdings;
    private final int quantityToDispose;
    private final CapabilityManager capabilityManager;

    public DisposeConsumableOperation(
        Holdings holdings,
        int quantityToDispose,
        CapabilityManager capabilityManager
    ) {
        this.holdings = java.util.Objects.requireNonNull(holdings, "holdings는 필수입니다");
        this.quantityToDispose = quantityToDispose;
        this.capabilityManager = java.util.Objects.requireNonNull(capabilityManager, "capabilityManager는 필수입니다");
    }

    public DisposeConsumableOperation(Holdings holdings, CapabilityManager capabilityManager) {
        this(holdings, 1, capabilityManager);
    }

    @Override
    public OperationResult execute(AssetItem asset) {
        java.util.Objects.requireNonNull(asset, "asset은 필수입니다");
        Optional<AssetItem> existingAsset = holdings.getAssets().stream()
                .filter(existing -> existing.getId().equals(asset.getId()))
                .findFirst();

        if (existingAsset.isEmpty()) {
            return OperationResult.failure(
                    asset.getId(),
                    asset.getName(),
                "보유하지 않은 자산입니다"
            );
        }

        AssetItem existing = existingAsset.get();

        if (!existing.hasCapability(Consumable.class)) {
            return OperationResult.failure(
                existing.getId(),
                existing.getName(),
                "자산을 다룰 수 없습니다."
            );
        }

        CapabilityResult result = capabilityManager.process(
            existing,
            Consumable.class,
            new QuantityCommand.Consume(quantityToDispose)
        );
        if (!result.isSuccess()) {
            return OperationResult.failure(
                existing.getId(),
                existing.getName(),
                result.getMessage()
            );
        }
        int remainingQuantity = existing.getCapability(Consumable.class)
            .orElseThrow(() -> new IllegalStateException("Consumable 능력 없음"))
            .quantity();


        if (remainingQuantity == 0)
            holdings.getAssets().remove(existing);

        return OperationResult.success(
                existing.getId(),
                existing.getName(),
                quantityToDispose + "개 처분, 남은 수량: " + remainingQuantity
        );
    }

    @Override
    public OperationType getType() {
        return OperationType.DISPOSE;
    }

    @Override
    public String getDescription() {
        return "소모성 자산 처분";
    }
}
