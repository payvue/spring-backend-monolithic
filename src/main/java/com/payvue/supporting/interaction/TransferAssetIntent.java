package com.payvue.supporting.interaction;

import com.payvue.core.asset.AssetItem;
import com.payvue.core.community.Participant;
import com.payvue.core.hold.Holdings;

public record TransferAssetIntent(
        Participant initiator,
        Holdings from,
        Holdings to,
        AssetItem item
) implements Intent, RequiresApproval {

    @Override
    public String getCategory() {
        return IntentCategory.TRANSFER.name();
    }

}
