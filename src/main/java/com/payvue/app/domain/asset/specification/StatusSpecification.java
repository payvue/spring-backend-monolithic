package com.payvue.app.domain.asset.specification;

import com.payvue.core.asset.AssetStatus;
import com.payvue.core.asset.AssetItem;
import com.payvue.core.hold.HoldingSpecification;

public class StatusSpecification implements HoldingSpecification {

    private final AssetStatus status;

    public StatusSpecification(AssetStatus status) {
        this.status = status;
    }

    @Override
    public boolean isSatisfiedBy(AssetItem asset) {
        return asset.getStatus().equals(status);
    }
}
