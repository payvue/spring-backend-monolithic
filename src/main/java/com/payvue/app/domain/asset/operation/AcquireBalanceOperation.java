package com.payvue.app.domain.asset.operation;

import com.payvue.characteristic.HasBalance;
import com.payvue.core.asset.AssetItem;
import com.payvue.core.asset.characteristic.CapabilityManager;
import com.payvue.core.asset.characteristic.CapabilityResult;
import com.payvue.core.asset.characteristic.command.DepositCommand;
import com.payvue.core.hold.AssetOperationUnit;
import com.payvue.core.hold.Holdings;
import com.payvue.core.hold.OperationResult;
import com.payvue.core.hold.OperationType;

import java.util.Optional;

public class AcquireBalanceOperation implements AssetOperationUnit {

    private final Holdings holdings;
    private final CapabilityManager capabilityManager;

    public AcquireBalanceOperation(Holdings holdings, CapabilityManager capabilityManager) {
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
            return mergeBalance(existingAsset.get(), asset);
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

    private OperationResult mergeBalance(AssetItem existing, AssetItem newAsset) {
        HasBalance newBalance = newAsset.getCapability(HasBalance.class)
            .orElseThrow(() -> new IllegalStateException("HasBalance 능력 없음"));

        CapabilityResult result = capabilityManager.process(
            existing,
            HasBalance.class,
            new DepositCommand.Deposit(newBalance.balance())
        );
        if (!result.isSuccess()) {
            return OperationResult.failure(
                existing.getId(),
                existing.getName(),
                result.getMessage()
            );
        }
        HasBalance updatedBalance = existing.getCapability(HasBalance.class)
            .orElseThrow(() -> new IllegalStateException("HasBalance 능력 없음"));

        return OperationResult.success(
            existing.getId(),
            existing.getName(),
            newBalance.balance() + " 추가, 총 잔액: " + updatedBalance.balance()
        );
    }

    @Override
    public OperationType getType() {
        return OperationType.ACQUIRE;
    }

    @Override
    public String getDescription() {
        return "금액성 자산 획득";
    }
}
