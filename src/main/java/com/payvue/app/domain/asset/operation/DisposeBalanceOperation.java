package com.payvue.app.domain.asset.operation;

import com.payvue.characteristic.HasBalance;
import com.payvue.core.asset.AssetItem;
import com.payvue.core.asset.characteristic.CapabilityManager;
import com.payvue.core.asset.characteristic.CapabilityResult;
import com.payvue.core.asset.characteristic.command.PayableCommand;
import com.payvue.core.hold.AssetOperationUnit;
import com.payvue.core.hold.OperationResult;
import com.payvue.core.hold.OperationType;
import com.payvue.shared.Money;

public class DisposeBalanceOperation implements AssetOperationUnit {

    private final Money amountToDeduct;
    private final CapabilityManager capabilityManager;

    public DisposeBalanceOperation(Money amountToDeduct, CapabilityManager capabilityManager) {
        this.amountToDeduct = java.util.Objects.requireNonNull(amountToDeduct, "amountToDeduct는 필수입니다");
        this.capabilityManager = java.util.Objects.requireNonNull(capabilityManager, "capabilityManager는 필수입니다");
    }

    @Override
    public OperationResult execute(AssetItem asset) {
        java.util.Objects.requireNonNull(asset, "asset은 필수입니다");
        HasBalance balance = asset.getCapability(HasBalance.class)
            .orElseThrow(() -> new IllegalStateException("HasBalance 능력 없음"));

        if (amountToDeduct.isNegative()) {
            return OperationResult.failure(
                asset.getId(),
                asset.getName(),
                "차감 금액이 음수입니다."
            );
        }

        if (balance.balance().isLessThan(amountToDeduct)) {
            return OperationResult.failure(
                    asset.getId(),
                    asset.getName(),
                    amountToDeduct + "차감 실패, 잔액: " + balance.balance()
            );
        }

        CapabilityResult result = capabilityManager.process(
            asset,
            HasBalance.class,
            new PayableCommand.Pay(amountToDeduct)
        );
        if (!result.isSuccess()) {
            return OperationResult.failure(
                asset.getId(),
                asset.getName(),
                result.getMessage()
            );
        }

        return OperationResult.success(
            asset.getId(),
            asset.getName(),
            amountToDeduct + " 차감 완료, 잔액: " + balance.balance()
        );
    }

    @Override
    public OperationType getType() {
        return OperationType.DEDUCT;
    }

    @Override
    public String getDescription() {
        return "잔액 차감: " + amountToDeduct;
    }
}
