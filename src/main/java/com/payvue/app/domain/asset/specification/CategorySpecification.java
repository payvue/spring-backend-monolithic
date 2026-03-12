package com.payvue.app.domain.asset.specification;

import com.payvue.core.asset.AssetClassification;
import com.payvue.core.asset.AssetItem;
import com.payvue.core.hold.HoldingSpecification;

public class CategorySpecification implements HoldingSpecification {

    private final AssetClassification category;

    public CategorySpecification(AssetClassification category) {
        this.category = category;
    }

    @Override
    public boolean isSatisfiedBy(AssetItem asset) {
        return asset.getCategory().equals(category);
    }
}
