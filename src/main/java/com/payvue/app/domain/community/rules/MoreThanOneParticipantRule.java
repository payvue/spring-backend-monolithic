package com.payvue.app.domain.community.rules;

import com.payvue.core.community.Community;
import com.payvue.core.community.policy.ChainableRule;

public class MoreThanOneParticipantRule extends ChainableRule {
    @Override
    public boolean evaluate(Community community) {
        return (1 < community.getParticipantCount());
    }
}
