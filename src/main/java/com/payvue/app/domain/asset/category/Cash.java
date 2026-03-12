package com.payvue.app.domain.asset.category;

import com.payvue.characteristic.HasBalance;
import com.payvue.core.asset.*;
import com.payvue.core.community.CommunityId;
import com.payvue.shared.Money;
import com.payvue.supporting.user.command.UserId;

import java.util.HashSet;


public final class Cash extends AssetItem implements AssetClassificationMarker {

    private Cash(
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
                AssetClassification.cash(),
                new HashSet<>(),
                status
        );
    }

    public static Cash create(
            CommunityId communityId,
            UserId contributorId,
            String name
    ) {
        return new Cash(
                AssetItemId.generate(),
                name,
                communityId,
                contributorId,
                AssetStatus.ACTIVE
        );
    }

    @Override
    public Cash with(com.payvue.characteristic.AssetCharacteristic capability) {
        super.with(capability);
        return this;
    }


    public static Cash reconstitute(
            AssetItemId id,
            String name,
            CommunityId communityId,
            UserId contributorId,
            AssetStatus status
    ) {
        return new Cash(
                id,
                name,
                communityId,
                contributorId,
                status
        );
    }

    public Money credit() {
        return getCapability(HasBalance.class)
                .orElseThrow(NotCapabilityException::new).balance();
    }
}
