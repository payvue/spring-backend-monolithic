package com.payvue.app.domain.asset.specification;

import com.payvue.characteristic.AssetCharacteristic;
import com.payvue.core.asset.AssetItem;
import com.payvue.core.hold.HoldingSpecification;

import java.util.Objects;

public class HasCapabilitySpecification implements HoldingSpecification {

    private final Class<? extends AssetCharacteristic> capabilityType;

    public HasCapabilitySpecification(Class<? extends AssetCharacteristic> capabilityType) {
        this.capabilityType = Objects.requireNonNull(capabilityType, "capabilityType is required");
    }

    @Override
    public boolean isSatisfiedBy(AssetItem asset) {
        return asset.hasCapability(capabilityType);
    }
}
