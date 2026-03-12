package com.payvue.app.domain.asset.category;

import com.payvue.core.asset.AssetClassification;
import com.payvue.core.asset.AssetItem;
import com.payvue.core.asset.AssetItemId;
import com.payvue.core.asset.AssetStatus;
import com.payvue.core.community.CommunityId;
import com.payvue.supporting.user.command.UserId;

import java.util.HashSet;


public final class Mileage extends AssetItem implements AssetClassificationMarker {

    private Mileage(
            AssetItemId id,
            String name,
            CommunityId communityId,
            UserId contributorId,
            AssetStatus status
    ) {
        super(
                id,
                name,
                communityId,
                contributorId,
                AssetClassification.mileage(),
                new HashSet<>(),
                status
        );
    }

    public static Mileage create(
            CommunityId communityId,
            UserId contributorId,
            String name
    ) {
        return new Mileage(
                AssetItemId.generate(),
                name,
                communityId,
                contributorId,
                AssetStatus.ACTIVE
        );
    }

    @Override
    public Mileage with(com.payvue.characteristic.AssetCharacteristic capability) {
        super.with(capability);
        return this;
    }

    public static Mileage reconstitute(
            AssetItemId id,
            String name,
            CommunityId communityId,
            UserId contributorId,
            AssetStatus status
    ) {
        return new Mileage(
                id,
                name,
                communityId,
                contributorId,
                status
        );
    }
}
