package com.payvue.supporting.interaction;

import com.payvue.core.community.Participant;

public record ManageIntent(
    Participant initiator,
    Participant participant
) implements Intent, RequiresAction, RequiresApproval {

    @Override
    public String getCategory() {
        return IntentCategory.MANAGE.name();
    }
}
