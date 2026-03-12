package com.payvue.app.domain.asset.operation;

import com.payvue.core.asset.AssetItem;
import com.payvue.core.hold.AssetOperationUnit;
import com.payvue.core.hold.OperationResult;
import com.payvue.core.hold.OperationType;

public class ExpireOperation implements AssetOperationUnit {

    private final String reason;

    public ExpireOperation(String reason) {
        this.reason = java.util.Objects.requireNonNull(reason, "reason은 필수입니다");
    }

    public ExpireOperation() {
        this("만료 기간 도래");
    }

    @Override
    public OperationResult execute(AssetItem asset) {
        java.util.Objects.requireNonNull(asset, "asset은 필수입니다");
        if (!asset.isActive()) {
            return OperationResult.failure(
                asset.getId(),
                asset.getName(),
                "이미 비활성 상태인 자산입니다"
            );
        }
        
        asset.expire();
        
        return OperationResult.success(
            asset.getId(),
            asset.getName(),
            "자산 만료: " + reason
        );
    }

    @Override
    public OperationType getType() {
        return OperationType.EXPIRE;
    }

    @Override
    public String getDescription() {
        return "자산 만료: " + reason;
    }
}
