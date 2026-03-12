package com.payvue.app.domain.asset.specification;

import com.payvue.core.asset.AssetItem;
import com.payvue.core.hold.HoldingSpecification;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class NamingSpecification implements HoldingSpecification {

    private final String name;

    @Override
    public boolean isSatisfiedBy(AssetItem asset) {
        return asset.getName().equals(name);
    }
}

