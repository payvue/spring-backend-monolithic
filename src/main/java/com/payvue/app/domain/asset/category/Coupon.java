package com.payvue.app.domain.asset.category;

import com.payvue.characteristic.AssetCharacteristic;
import com.payvue.core.asset.AssetClassification;
import com.payvue.core.asset.AssetItem;
import com.payvue.core.asset.AssetItemId;
import com.payvue.core.asset.AssetStatus;
import com.payvue.core.community.CommunityId;
import com.payvue.supporting.user.command.UserId;

import java.util.HashSet;

public final class Coupon extends AssetItem implements AssetClassificationMarker {

    private Coupon(
            AssetItemId id,
            String name,
            CommunityId communityId,
            UserId contributorId,
            AssetStatus status
    ) {
        super(id, name, communityId, contributorId, AssetClassification.coupon(), new HashSet<>(), status);
    }

    public static Coupon create(CommunityId communityId, UserId contributorId, String name) {
        return new Coupon(
                AssetItemId.generate(),
                name,
                communityId,
                contributorId,
                AssetStatus.ACTIVE
        );
    }

    @Override
    public Coupon with(AssetCharacteristic capability) {
        super.with(capability);
        return this;
    }
}
