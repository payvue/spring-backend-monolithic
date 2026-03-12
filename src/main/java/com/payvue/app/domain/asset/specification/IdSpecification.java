package com.payvue.app.domain.asset.specification;

import com.payvue.core.asset.AssetItem;
import com.payvue.core.asset.AssetItemId;
import com.payvue.core.hold.HoldingSpecification;
import lombok.AllArgsConstructor;

import java.util.Objects;

@AllArgsConstructor
public class IdSpecification implements HoldingSpecification {

    private final AssetItemId id;

    @Override
    public boolean isSatisfiedBy(AssetItem asset) {
        return Objects.equals(asset.getId(), id);
    }
}
