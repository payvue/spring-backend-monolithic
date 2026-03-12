package com.payvue.supporting.interaction;

import com.payvue.core.community.Community;
import com.payvue.core.community.Participant;
import com.payvue.core.community.policy.SingleRulePolicy;
import com.payvue.app.domain.community.rules.HasDecisionAuthorityRule;

final class InviteMemberHandler implements InteractionHandler<ManageIntent> {

  @Override
  public boolean supports(Decision<?> decision) {
    return decision instanceof InviteDecision;
  }

  @Override
  public void handle(InteractionContext context, Decision<? extends ManageIntent> decision) {
    if (!(context instanceof InternalInteractionContext internal)) {
      throw new IllegalArgumentException("내부 컨텍스트가 필요합니다.");
    }

    Participant initiator = decision.getIntent().initiator();
    Participant member = decision.getIntent().participant();
    Community community = internal.getCommunity();

    if (community.isParticipant(member.getUserId())) {
      throw new IllegalArgumentException("이미 참여 중인 사용자입니다.");
    }

    community.addMember(
        member,
        SingleRulePolicy.of(new HasDecisionAuthorityRule(initiator.getUserId()))
    );
    community.changeParticipantRole(
        member.getUserId(),
        member.getRole(),
        SingleRulePolicy.of(new HasDecisionAuthorityRule(initiator.getUserId()))
    );
    member.pending();
  }
}
