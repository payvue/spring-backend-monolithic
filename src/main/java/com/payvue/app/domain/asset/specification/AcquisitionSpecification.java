package com.payvue.app.domain.asset.specification;

import com.payvue.characteristic.Acquirable;
import com.payvue.core.asset.AssetItem;
import com.payvue.core.hold.HoldingSpecification;

import java.util.Objects;

public class AcquisitionSpecification implements HoldingSpecification {

    private final Acquirable target;

    public AcquisitionSpecification(Acquirable target) {
        Objects.requireNonNull(target, "acquirable은 필수입니다");
        this.target = target;
    }

    @Override
    public boolean isSatisfiedBy(AssetItem asset) {
        return asset.getCapability(Acquirable.class)
                .map(acquirable -> acquirable.method() == target.method())
                .orElse(false);
    }
}
