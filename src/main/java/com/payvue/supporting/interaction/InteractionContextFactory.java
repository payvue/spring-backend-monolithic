package com.payvue.supporting.interaction;

import com.payvue.core.community.Community;
import com.payvue.core.relation.CommunityRelation;

public class InteractionContextFactory {

  public InteractionContext createInternal(Decision<?> decision, Community community) {
    return new InternalInteractionContext(community);
  }

  public InteractionContext createCross(
      Decision<?> decision,
      Community from,
      Community to,
      CommunityRelation relation
  ) {
    return new CrossCommunityInteractionContext(from, to, relation);
  }
}
