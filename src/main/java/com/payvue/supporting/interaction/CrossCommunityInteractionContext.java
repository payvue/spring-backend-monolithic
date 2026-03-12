package com.payvue.supporting.interaction;

import com.payvue.app.domain.community.rules.HasDecisionAuthorityRule;
import com.payvue.app.domain.community.rules.IsParticipantRule;
import com.payvue.core.community.Community;
import com.payvue.core.community.Participant;
import com.payvue.core.community.policy.ChainedRulePolicy;
import com.payvue.core.community.policy.CommunityPolicy;
import com.payvue.core.community.policy.SingleRulePolicy;
import com.payvue.core.relation.CommunityRelation;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PACKAGE)
class CrossCommunityInteractionContext implements InteractionContext {

  private final Community sourceCommunity;
  private final Community targetCommunity;
  private final CommunityRelation relation;

  @Override
  public Interaction execute(Decision<?> decision) {
    throw new UnsupportedOperationException(
        "InteractionContext는 실행 책임을 가지지 않습니다. InteractionExecutor를 사용하세요."
    );
  }

  @Override
  public boolean supports(Decision<?> decision, DomainService service) {
    return service.supports(decision);
  }

  @Override
  public boolean validate(Decision<?> decision) {
    Intent intent = decision.getIntent();
    Participant initiator = intent.initiator();

    ChainedRulePolicy.Builder builder = new ChainedRulePolicy.Builder()
        .append(new IsParticipantRule(initiator.getUserId()));

    if (intent instanceof RequiresAction || intent instanceof RequiresApproval) {

      if (!initiator.isAdmin()) {
        return false;
      }
    }

    CommunityPolicy sourcePolicy = builder.build();
    return sourcePolicy.isSatisfied(sourceCommunity) && relation.isActive();
  }
}
