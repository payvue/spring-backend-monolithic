package com.payvue.supporting.interaction;

import static org.assertj.core.api.Assertions.assertThat;

import com.payvue.app.domain.community.Cooperative;
import com.payvue.app.domain.community.Personal;
import com.payvue.app.domain.community.Role;
import com.payvue.app.domain.community.rules.HasDecisionAuthorityRule;
import com.payvue.core.community.Community;
import com.payvue.core.community.Participant;
import com.payvue.core.community.policy.ChainedRulePolicy;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ChangeRoleHandlerTest {

  @Test
  @DisplayName("역할 변경 결정은 대상 참여자의 역할을 갱신한다")
  void changeRoleUpdatesParticipantRole() {
    UUID adminId = UUID.randomUUID();
    UUID memberId = UUID.randomUUID();
    Community community = Cooperative.create(
        "org",
        "desc",
        adminId,
        Role.ADMIN,
        Role.MEMBER
    );
    community.addMember(memberId, ChainedRulePolicy.builder()
        .append(new HasDecisionAuthorityRule(adminId))
        .build());

    Participant initiator = Participant.of(adminId, Role.ADMIN);
    Participant member = Participant.of(memberId, Role.MEMBER);
    ChangeRoleDecision decision = new ChangeRoleDecision(new ManageIntent(initiator, member), Role.GUEST);

    ChangeRoleHandler handler = new ChangeRoleHandler();
    InternalInteractionContext context = new InternalInteractionContext(community);
    handler.handle(context, decision);

    assertThat(community.getParticipantRole(memberId)).isEqualTo(Role.GUEST);
    assertThat(community.getParticipantCount()).isEqualTo(2);
  }

  @Test
  @DisplayName("결정 타입이 일치하지 않으면 예외가 발생한다")
  void changeRoleThrowsWhenDecisionTypeMismatch() {
    UUID adminId = UUID.randomUUID();
    UUID memberId = UUID.randomUUID();
    Community community = Cooperative.create(
        "org",
        "desc",
        adminId,
        Role.ADMIN,
        Role.MEMBER
    );
    Community target = Cooperative.create(
        "org2",
        "desc2",
        UUID.randomUUID(),
        Role.ADMIN,
        Role.MEMBER
    );
    Participant initiator = Participant.of(adminId, Role.ADMIN);
    Participant member = Participant.of(memberId, Role.MEMBER);
    InviteDecision decision = new InviteDecision(new ManageIntent(initiator, member));

    ChangeRoleHandler handler = new ChangeRoleHandler();
    InternalInteractionContext context = new InternalInteractionContext(community);

    org.assertj.core.api.Assertions.assertThatThrownBy(() -> handler.handle(context, decision))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("내부 컨텍스트가 아니면 예외가 발생한다")
  void changeRoleThrowsWhenContextIsNotInternal() {
    UUID adminId = UUID.randomUUID();
    UUID memberId = UUID.randomUUID();
    Community community = Personal.create(
        "personal",
        "desc",
        adminId,
        Role.ADMIN
    );

    Community target = Personal.create(
        "personal2",
        "desc2",
        UUID.randomUUID(),
        Role.ADMIN
    );
    Participant initiator = Participant.of(adminId, Role.ADMIN);
    Participant member = Participant.of(memberId, Role.MEMBER);
    ChangeRoleDecision decision = new ChangeRoleDecision(new ManageIntent(initiator, member), Role.GUEST);
    CrossCommunityInteractionContext context = new CrossCommunityInteractionContext(
        community,
        target,
        com.payvue.core.relation.CommunityRelation.associateWith(community, target)
    );

    ChangeRoleHandler handler = new ChangeRoleHandler();

    org.assertj.core.api.Assertions.assertThatThrownBy(() -> handler.handle(context, decision))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
