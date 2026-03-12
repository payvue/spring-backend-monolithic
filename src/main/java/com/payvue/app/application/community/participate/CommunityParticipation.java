package com.payvue.app.application.community.participate;

import com.payvue.app.presentation.rest.community.CommunityParticipatingResponse;
import com.payvue.core.community.Community;
import com.payvue.core.community.CommunityId;
import com.payvue.supporting.user.command.User;
import com.payvue.supporting.user.command.UserAccountView;
import com.payvue.supporting.user.command.UserId;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.security.core.userdetails.UserDetails;

public final class CommunityParticipation {
  private final UserDetails userDetails;
  private final boolean standAlone;
  private User user;
  private Set<CommunityId> communityIds = Collections.emptySet();
  private List<Community> communities = List.of();
  private Map<UserId, UserAccountView> userViews = Map.of();
  private Map<CommunityId, Long> balances = Map.of();
  private CommunityParticipatingResponse response;

  public CommunityParticipation(UserDetails userDetails, boolean standAlone) {
    this.userDetails = userDetails;
    this.standAlone = standAlone;
  }

  public UserDetails userDetails() {
    return userDetails;
  }

  public boolean standAlone() {
    return standAlone;
  }

  public User user() {
    return user;
  }

  public void user(User user) {
    this.user = user;
  }

  public Set<CommunityId> communityIds() {
    return communityIds;
  }

  public void communityIds(Set<CommunityId> communityIds) {
    this.communityIds = communityIds == null ? Collections.emptySet() : communityIds;
  }

  public List<Community> communities() {
    return communities;
  }

  public void communities(List<Community> communities) {
    this.communities = communities == null ? List.of() : communities;
  }

  public Map<UserId, UserAccountView> userViews() {
    return userViews;
  }

  public void userViews(Map<UserId, UserAccountView> userViews) {
    this.userViews = userViews == null ? Map.of() : userViews;
  }

  public Map<CommunityId, Long> balances() {
    return balances;
  }

  public void balances(Map<CommunityId, Long> balances) {
    this.balances = balances == null ? Map.of() : balances;
  }

  public CommunityParticipatingResponse response() {
    return response;
  }

  public void response(CommunityParticipatingResponse response) {
    this.response = response;
  }
}
