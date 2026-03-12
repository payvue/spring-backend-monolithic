package com.payvue.app.application.asset.online;

import com.payvue.app.presentation.rest.hold.AssetOnlineItemResponse;
import com.payvue.core.community.Community;
import com.payvue.core.community.CommunityId;
import com.payvue.supporting.user.command.User;
import java.util.List;
import java.util.Set;
import org.springframework.security.core.userdetails.UserDetails;

public final class OnlineAsset {
  private final UserDetails userDetails;
  private final String category;
  private final CommunityId communityId;
  private User user;
  private Set<Community> communities = Set.of();
  private List<AssetOnlineItemResponse> responses = List.of();

  public OnlineAsset(UserDetails userDetails, String category, CommunityId communityId) {
    this.userDetails = userDetails;
    this.category = category;
    this.communityId = communityId;
  }

  public UserDetails userDetails() {
    return userDetails;
  }

  public String category() {
    return category;
  }

  public CommunityId communityId() {
    return communityId;
  }

  public User user() {
    return user;
  }

  public void user(User user) {
    this.user = user;
  }

  public Set<Community> communities() {
    return communities;
  }

  public void communities(Set<Community> communities) {
    this.communities = communities == null ? Set.of() : communities;
  }

  public List<AssetOnlineItemResponse> responses() {
    return responses;
  }

  public void responses(List<AssetOnlineItemResponse> responses) {
    this.responses = responses == null ? List.of() : responses;
  }
}
