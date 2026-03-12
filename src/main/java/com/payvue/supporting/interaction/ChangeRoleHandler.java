package com.payvue.supporting.interaction;

import com.payvue.core.community.Community;
import com.payvue.core.community.Participant;
import com.payvue.core.community.policy.ChainedRulePolicy;
import com.payvue.app.domain.community.rules.HasDecisionAuthorityRule;
import com.payvue.app.domain.community.rules.MoreThanOneParticipantRule;

final class ChangeRoleHandler implements InteractionHandler<ManageIntent> {

  @Override
  public boolean supports(Decision<?> decision) {
    return decision instanceof ChangeRoleDecision;
  }

  @Override
  public void handle(InteractionContext context, Decision<? extends ManageIntent> decision) {
    if (!(context instanceof InternalInteractionContext internal)) {
      throw new IllegalArgumentException("내부 컨텍스트가 필요합니다.");
    }
    if (!(decision instanceof ChangeRoleDecision changeRoleDecision)) {
      throw new IllegalArgumentException("결정 타입이 올바르지 않습니다.");
    }

    Participant initiator = decision.getIntent().initiator();
    Participant member = decision.getIntent().participant();
    Community community = internal.getCommunity();

    community.changeParticipantRole(
        member.getUserId(),
        changeRoleDecision.getNewRole(),
        ChainedRulePolicy.builder()
            .append(new HasDecisionAuthorityRule(initiator.getUserId()))
            .append(new MoreThanOneParticipantRule())
            .build()
    );
  }
}
