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

public class AcquireConsumableOperation implements AssetOperationUnit {

    private final Holdings holdings;
    private final CapabilityManager capabilityManager;

    public AcquireConsumableOperation(Holdings holdings, CapabilityManager capabilityManager) {
        this.holdings = java.util.Objects.requireNonNull(holdings, "holdings는 필수입니다");
        this.capabilityManager = java.util.Objects.requireNonNull(capabilityManager, "capabilityManager는 필수입니다");
    }

    @Override
    public OperationResult execute(AssetItem asset) {
        java.util.Objects.requireNonNull(asset, "asset은 필수입니다");
        Optional<AssetItem> existingAsset = holdings.getAssets().stream()
                .filter(existing -> isSameAsset(existing, asset))
                .findFirst();

        if (existingAsset.isPresent()) {
            return mergeQuantity(existingAsset.get(), asset);
        } else {
            holdings.getAssets().add(asset);
            return OperationResult.success(
                    asset.getId(),
                    asset.getName(),
                "새 자산 획득 완료"
            );
        }
    }

    private boolean isSameAsset(AssetItem existing, AssetItem newAsset) {
        return existing.getName().equals(newAsset.getName())
                && existing.getCategory().equals(newAsset.getCategory());
    }

    private OperationResult mergeQuantity(AssetItem existing, AssetItem newAsset) {
        if (!existing.hasCapability(Consumable.class) || !newAsset.hasCapability(Consumable.class)) {
            return OperationResult.failure(
                existing.getId(),
                existing.getName(),
                "Consumable 능력이 없는 자산입니다"
            );
        }

        Consumable existingConsumable = existing.getCapability(Consumable.class).orElseThrow();
        Consumable newConsumable = newAsset.getCapability(Consumable.class).orElseThrow();

        int addedQuantity = newConsumable.quantity();
        CapabilityResult result = capabilityManager.process(
            existing,
            Consumable.class,
            new QuantityCommand.Produce(addedQuantity)
        );
        if (!result.isSuccess()) {
            return OperationResult.failure(
                existing.getId(),
                existing.getName(),
                result.getMessage()
            );
        }
        int totalQuantity = existingConsumable.quantity();

        return OperationResult.success(
            existing.getId(),
            existing.getName(),
            addedQuantity + "개 추가, 총 수량: " + totalQuantity
        );
    }

    @Override
    public OperationType getType() {
        return OperationType.ACQUIRE;
    }

    @Override
    public String getDescription() {
        return "소모성 자산 획득";
    }
}
