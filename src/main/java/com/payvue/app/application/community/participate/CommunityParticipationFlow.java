package com.payvue.app.application.community.participate;

import com.payvue.app.domain.asset.OnlineCashManager;
import com.payvue.app.presentation.rest.community.CommunityParticipatingResponse;
import com.payvue.core.community.Community;
import com.payvue.core.community.CommunityContextManager;
import com.payvue.core.community.CommunityId;
import com.payvue.shared.Money;
import com.payvue.supporting.user.UserContextManager;
import com.payvue.supporting.user.command.User;
import com.payvue.supporting.user.command.UserAccountView;
import com.payvue.supporting.user.command.UserId;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.springframework.security.core.userdetails.UserDetails;

public final class CommunityParticipationFlow {
  private final CommunityParticipation context;

  private CommunityParticipationFlow(CommunityParticipation context) {
    this.context = context;
  }

  public static CommunityParticipationFlow start(UserDetails userDetails, boolean standAlone) {
    return new CommunityParticipationFlow(new CommunityParticipation(userDetails, standAlone));
  }

  public CommunityParticipationFlow user(Consumer<CommunityParticipationFlow> step) {
    step.accept(this);
    return this;
  }

  public CommunityParticipationFlow translate(Consumer<CommunityParticipationFlow> step) {
    step.accept(this);
    return this;
  }

  public CommunityParticipationFlow community(Consumer<CommunityParticipationFlow> step) {
    step.accept(this);
    return this;
  }

  public CommunityParticipationFlow map(Consumer<CommunityParticipationFlow> step) {
    step.accept(this);
    return this;
  }

  public CommunityParticipationFlow asset(Consumer<CommunityParticipationFlow> step) {
    step.accept(this);
    return this;
  }

  public CommunityParticipationFlow result(Consumer<CommunityParticipationFlow> step) {
    step.accept(this);
    return this;
  }

  public CommunityParticipationFlow loadUser(UserContextManager userContextManager) {
    User user = userContextManager.findByEmail(context.userDetails().getUsername(), User.class)
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    context.user(user);
    return this;
  }

  public CommunityParticipationFlow resolveCommunityIds() {
    User user = context.user();
    UserAccountView view = UserAccountView.from(user);
    context.communityIds(view.membershipsByCommunity().keySet().stream()
            .map(CommunityId::of)
            .collect(Collectors.toSet()));
    return this;
  }

  public CommunityParticipationFlow loadCommunities(CommunityContextManager communityContextManager) {
    Set<com.payvue.core.community.CommunityId> ids = context.communityIds();
    if (ids.isEmpty()) {
      context.communities(List.of());
      return this;
    }
    context.communities(List.copyOf(communityContextManager.resolve(context.user())));
    return this;
  }

  public CommunityParticipationFlow buildUserViews(UserContextManager userContextManager) {
    List<Community> communities = context.communities();
    if (communities.isEmpty()) {
      context.userViews(Map.of());
      return this;
    }
    Map<UserId, UserAccountView> views = new HashMap<>();
    Set<UserId> ids = new HashSet<>();
    for (Community community : communities) {
      if (community == null) {
        continue;
      }
      for (UUID participantId : community.getParticipantIds()) {
        ids.add(UserId.of(participantId));
      }
    }
    for (UserId id : ids) {
      userContextManager.findById(id, UserAccountView.class)
          .ifPresent(view -> views.put(id, view));
    }
    User user = context.user();
    if (user != null) {
      views.put(user.getId(), UserAccountView.from(user));
    }
    context.userViews(views);
    return this;
  }

  public CommunityParticipationFlow loadBalances(OnlineCashManager cashManager) {
    List<Community> communities = context.communities();
    if (communities.isEmpty()) {
      context.balances(Map.of());
      return this;
    }
    Map<CommunityId, Long> balances = new HashMap<>();
    for (Community community : communities) {
      if (community == null || community.getId() == null) {
        continue;
      }
      Money balance = cashManager.possesses(community);
      balances.put(community.getId(), balance == null ? 0L : balance.toLong());
    }
    context.balances(balances);
    return this;
  }

  public CommunityParticipationFlow toResponse() {
    if (context.communityIds().isEmpty()) {
      context.response(new CommunityParticipatingResponse(false, List.of()));
      return this;
    }
    context.response(CommunityParticipatingResponse.from(
        context.communities(),
        context.userViews(),
        context.balances(),
        context.user().getId(),
        context.standAlone()
    ));
    return this;
  }

  public CommunityParticipatingResponse end() {
    return context.response();
  }
}
