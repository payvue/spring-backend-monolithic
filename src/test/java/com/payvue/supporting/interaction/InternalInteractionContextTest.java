package com.payvue.supporting.interaction;

import static org.assertj.core.api.Assertions.assertThat;

import com.payvue.app.domain.community.Cooperative;
import com.payvue.app.domain.community.Role;
import com.payvue.core.community.Community;
import com.payvue.core.community.Participant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class InternalInteractionContextTest {

  @Test
  @DisplayName("관리 권한을 가진 참여자는 내부 컨텍스트 검증을 통과한다")
  void validatePassesForAdminParticipant() {
    UUID adminId = UUID.randomUUID();
    Community community = Cooperative.create(
        "org",
        "desc",
        adminId,
        Role.ADMIN,
        Role.MEMBER
    );
    Participant initiator = Participant.of(adminId, Role.ADMIN);
    Participant member = Participant.of(UUID.randomUUID(), Role.MEMBER);
    InviteDecision decision = new InviteDecision(new ManageIntent(initiator, member));

    InternalInteractionContext context = new InternalInteractionContext(community);

    assertThat(context.validate(decision)).isTrue();
  }

  @Test
  @DisplayName("관리 권한이 없는 참여자는 내부 컨텍스트 검증에 실패한다")
  void validateFailsForNonAdminParticipant() {
    UUID adminId = UUID.randomUUID();
    UUID memberId = UUID.randomUUID();
    Community community = Cooperative.create(
        "org",
        "desc",
        adminId,
        Role.ADMIN,
        Role.MEMBER
    );
    community.addMember(
        memberId,
        com.payvue.core.community.policy.SingleRulePolicy.of(
            new com.payvue.app.domain.community.rules.HasDecisionAuthorityRule(adminId)
        )
    );
    Participant initiator = Participant.of(memberId, Role.MEMBER);
    Participant member = Participant.of(UUID.randomUUID(), Role.MEMBER);
    InviteDecision decision = new InviteDecision(new ManageIntent(initiator, member));

    InternalInteractionContext context = new InternalInteractionContext(community);

    assertThat(context.validate(decision)).isFalse();
  }
}
