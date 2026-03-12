package com.payvue.core.community;

import java.time.Instant;
import java.util.*;

import com.payvue.supporting.user.command.User;
import com.payvue.supporting.user.command.UserAccountView;
import com.payvue.supporting.user.command.UserId;
import com.payvue.supporting.user.command.UserRepository;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class MemberShipTranslator implements CommunityResolver {

  private final UserRepository userRepository;

  public Set<CommunityId> translate(User user) {
    if (user == null) {
      return Set.of();
    }
    Set<CommunityId> ids = new HashSet<>();
    for (Map.Entry<UUID, Instant> entry : UserAccountView.from(user).membershipsByCommunity().entrySet()) {
      ids.add(CommunityId.of(entry.getKey()));
    }

    return ids;
  }

  @Override
  public Set<CommunityId> resolveIds(User user) {
    return translate(user);
  }

  public Map<UserId, UserAccountView> translate(User user, List<Community> communities) {
    Map<UserId, UserAccountView> userViews = translate(communities);
    if (user != null) {
      userViews.put(user.getId(), UserAccountView.from(user));
    }
    return userViews;
  }

  public Map<UserId, UserAccountView> translate(List<Community> communities) {
    Map<UserId, UserAccountView> views = new HashMap<>();
    for (Community community : communities) {
      if (community == null) {
        continue;
      }
      views.putAll(translate(community));
    }
    return views;
  }

  public Map<UserId, UserAccountView> translate(Community community) {
    Map<UserId, UserAccountView> views = new HashMap<>();
    if (community == null) {
      return views;
    }
    Set<UserId> userIds = new HashSet<>();
    for (UUID participantId : community.getParticipantIds()) {
      userIds.add(UserId.of(participantId));
    }
    for (UserId userId : userIds) {
      userRepository.findById(userId)
          .map(UserAccountView::from)
          .ifPresent(view -> views.put(userId, view));
    }
    return views;
  }

}
