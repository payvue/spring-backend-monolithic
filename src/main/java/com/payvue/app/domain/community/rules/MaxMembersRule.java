package com.payvue.app.domain.community.rules;

import com.payvue.core.community.Community;
import com.payvue.core.community.policy.ChainableRule;

public class MaxMembersRule extends ChainableRule {

    private final int maxMembers;

    public MaxMembersRule(int maxMembers) {
        if (maxMembers <= 0) {
            throw new IllegalArgumentException("최대 멤버 수는 0보다 커야 합니다.");
        }
        this.maxMembers = maxMembers;
    }

    @Override
    public boolean evaluate(Community community) {
        return community.getParticipantCount() < maxMembers;
    }
}
