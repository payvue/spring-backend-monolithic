package com.payvue.app.domain.community.rules;

import java.util.UUID;

import com.payvue.core.community.Community;
import com.payvue.core.community.policy.ChainableRule;
import com.payvue.core.community.policy.Rule;

public class HasDecisionAuthorityRule extends ChainableRule {

    private final UUID userId;
    private Rule isParticipantRule;

    public HasDecisionAuthorityRule(UUID userId) {
        if (userId == null) {
            throw new IllegalArgumentException("사용자 ID는 null일 수 없습니다.");
        }
        this.userId = userId;
        this.isParticipantRule = new IsParticipantRule(userId);
    }

    @Override
    public boolean evaluate(Community community) {
        return isParticipantRule.evaluate(community) && community.isAdmin(userId);
    }
}
