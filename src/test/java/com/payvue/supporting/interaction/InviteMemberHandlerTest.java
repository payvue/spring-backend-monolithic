package com.payvue.supporting.interaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.payvue.app.domain.community.Cooperative;
import com.payvue.app.domain.community.Personal;
import com.payvue.app.domain.community.Role;
import com.payvue.app.domain.community.rules.HasDecisionAuthorityRule;
import com.payvue.core.community.Community;
import com.payvue.core.community.Participant;
import java.util.UUID;

import com.payvue.core.community.policy.SingleRulePolicy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class InviteMemberHandlerTest {

  @Test
  @DisplayName("초대 결정은 커뮤니티에 멤버를 추가하고 초대 대기 상태로 만든다")
  void inviteAddsMemberAndSetsPending() {
    UUID adminId = UUID.randomUUID();
    UUID memberId = UUID.randomUUID();
    Community community = Cooperative.create(
        "org",
        "desc",
        adminId,
        Role.ADMIN,
        Role.MEMBER
    );
    Participant initiator = Participant.of(adminId, Role.ADMIN);
    Participant member = Participant.of(memberId, Role.MEMBER);

    InviteMemberHandler handler = new InviteMemberHandler();
    InternalInteractionContext context = new InternalInteractionContext(community);
    InviteDecision decision = new InviteDecision(new ManageIntent(initiator, member));

    handler.handle(context, decision);

    assertThat(community.isParticipant(memberId)).isTrue();
    assertThat(community.getParticipantRole(memberId)).isEqualTo(Role.MEMBER);
    assertThat(community.getParticipantCount()).isEqualTo(2);
    assertThat(member.isPending()).isTrue();
  }

  @Test
  @DisplayName("이미 참여 중인 사용자를 초대하면 예외가 발생한다")
  void inviteThrowsWhenAlreadyParticipant() {
    UUID adminId = UUID.randomUUID();
    UUID memberId = UUID.randomUUID();
    Community community = Cooperative.create(
        "org",
        "desc",
        adminId,
        Role.ADMIN,
        Role.MEMBER
    );
    community.addMember(memberId, SingleRulePolicy.of(new HasDecisionAuthorityRule(adminId)));
    Participant initiator = Participant.of(adminId, Role.ADMIN);
    Participant member = Participant.of(memberId, Role.MEMBER);

    InviteMemberHandler handler = new InviteMemberHandler();
    InternalInteractionContext context = new InternalInteractionContext(community);
    InviteDecision decision = new InviteDecision(new ManageIntent(initiator, member));

    assertThatThrownBy(() -> handler.handle(context, decision))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("내부 컨텍스트가 아니면 예외가 발생한다")
  void inviteThrowsWhenContextIsNotInternal() {
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
    InviteDecision decision = new InviteDecision(new ManageIntent(initiator, member));
    CrossCommunityInteractionContext context = new CrossCommunityInteractionContext(
        community,
        target,
        com.payvue.core.relation.CommunityRelation.associateWith(community, target)
    );

    InviteMemberHandler handler = new InviteMemberHandler();

    assertThatThrownBy(() -> handler.handle(context, decision))
        .isInstanceOf(IllegalArgumentException.class);
  }

}
