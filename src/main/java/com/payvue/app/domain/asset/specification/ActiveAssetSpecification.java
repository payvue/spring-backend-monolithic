package com.payvue.app.domain.asset.specification;

import com.payvue.core.asset.AssetItem;
import com.payvue.core.hold.HoldingSpecification;

public class ActiveAssetSpecification implements HoldingSpecification {

    @Override
    public boolean isSatisfiedBy(AssetItem asset) {
        return asset.isActive();
    }
}
