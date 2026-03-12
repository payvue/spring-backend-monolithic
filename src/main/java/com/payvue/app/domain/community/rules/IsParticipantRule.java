package com.payvue.app.domain.community.rules;

import java.util.UUID;

import com.payvue.core.community.Community;
import com.payvue.core.community.policy.ChainableRule;

/**
 * IsParticipantRule - 커뮤니티 구성원 여부 확인 규칙
 * 
 * 지정된 사용자가 커뮤니티의 구성원인지 검증합니다.
 */
public class IsParticipantRule extends ChainableRule {

    private final UUID userId;

    public IsParticipantRule(UUID userId) {
        if (userId == null) {
            throw new IllegalArgumentException("사용자 ID는 null일 수 없습니다.");
        }
        this.userId = userId;
    }

    @Override
    public boolean evaluate(Community community) {
        return community.isParticipant(userId);
    }

    public UUID getUserId() {
        return userId;
    }
}
