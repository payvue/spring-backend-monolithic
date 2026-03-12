package com.payvue.supporting.interaction;

import com.payvue.core.asset.AssetItem;
import com.payvue.core.community.Participant;
import com.payvue.core.hold.Holdings;

public record ExecutePaymentIntent(
        Participant initiator,
        Holdings holdings,
        AssetItem item
) implements Intent, RequiresApproval {

    @Override
    public String getCategory() {
        return IntentCategory.PAYMENT.name();
    }
}

