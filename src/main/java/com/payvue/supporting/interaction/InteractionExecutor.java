package com.payvue.supporting.interaction;

import com.payvue.core.community.Community;
import com.payvue.core.relation.CommunityRelation;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class InteractionExecutor {

  private final InteractionContextFactory interactionContextFactory;
  private final InteractionFlowRegistry moduleRegistry;

  public Interaction executeInternal(Decision<?> decision, Community community) {
    InteractionContext context = interactionContextFactory.createInternal(decision, community);
    InteractionFlow flow = moduleRegistry.resolve(decision);
    return flow.execute(context, decision);
  }

  public Interaction executeCross(
      Decision<?> decision,
      Community from,
      Community to,
      CommunityRelation relation
  ) {
    InteractionContext context = interactionContextFactory.createCross(decision, from, to, relation);
    InteractionFlow flow = moduleRegistry.resolve(decision);
    return flow.execute(context, decision);
  }
}
