package com.payvue.app.application.asset.online;

import com.payvue.app.domain.asset.OnlineAssetService;
import com.payvue.app.domain.community.CommunityCategory;
import com.payvue.app.presentation.rest.hold.AssetOnlineItemResponse;
import com.payvue.core.asset.AssetItem;
import com.payvue.core.community.Community;
import com.payvue.core.community.CommunityContextManager;
import com.payvue.core.community.CommunityId;
import com.payvue.supporting.user.UserContextManager;
import com.payvue.supporting.user.command.User;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import org.springframework.security.core.userdetails.UserDetails;

public final class OnlineAssetFlow {
  private final OnlineAsset context;

  private OnlineAssetFlow(OnlineAsset context) {
    this.context = context;
  }

  public static OnlineAssetFlow start(UserDetails userDetails) {
    return new OnlineAssetFlow(new OnlineAsset(userDetails, null, null));
  }

  public static OnlineAssetFlow startWithCategory(UserDetails userDetails, String category) {
    return new OnlineAssetFlow(new OnlineAsset(userDetails, category, null));
  }

  public static OnlineAssetFlow startWithCommunityId(UserDetails userDetails, CommunityId communityId) {
    return new OnlineAssetFlow(new OnlineAsset(userDetails, null, communityId));
  }

  public OnlineAssetFlow user(Consumer<OnlineAssetFlow> step) {
    step.accept(this);
    return this;
  }

  public OnlineAssetFlow community(Consumer<OnlineAssetFlow> step) {
    step.accept(this);
    return this;
  }

  public OnlineAssetFlow asset(Consumer<OnlineAssetFlow> step) {
    step.accept(this);
    return this;
  }

  public OnlineAssetFlow map(Consumer<OnlineAssetFlow> step) {
    step.accept(this);
    return this;
  }

  public OnlineAssetFlow loadUser(UserContextManager userContextManager) {
    User user = userContextManager.findByEmail(context.userDetails().getUsername(), User.class)
        .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    context.user(user);
    return this;
  }

  public OnlineAssetFlow loadPersonalCommunity(CommunityContextManager communityContextManager) {
    context.communities(communityContextManager.resolve(context.user(), CommunityCategory.PERSONAL.name()));
    return this;
  }

  public OnlineAssetFlow loadCommunitiesByCategory(CommunityContextManager communityContextManager) {
    String category = context.category();
    if (category == null || category.isBlank()) {
      throw new IllegalArgumentException("category는 비어 있을 수 없습니다.");
    }
    Set<Community> communities = communityContextManager.resolve(context.user(), category);
    context.communities(communities);
    return this;
  }

  public OnlineAssetFlow loadCommunitiesById(CommunityContextManager communityContextManager) {
    CommunityId id = context.communityId();
    if (id == null) {
      throw new IllegalArgumentException("communityId는 필수입니다.");
    }
    Set<Community> communities = communityContextManager.resolve(context.user());
    if (communities == null || communities.isEmpty()) {
      throw new IllegalArgumentException("커뮤니티를 찾을 수 없습니다.");
    }
    Community selected = null;
    for (Community community : communities) {
      if (community != null && id.equals(community.getId())) {
        selected = community;
        break;
      }
    }
    if (selected == null) {
      throw new IllegalArgumentException("커뮤니티를 찾을 수 없습니다.");
    }
    context.communities(Set.of(selected));
    return this;
  }

  public OnlineAssetFlow loadOnlineAssets(OnlineAssetService onlineAssetService) {
    Set<Community> communities = context.communities();
    if (communities == null || communities.isEmpty()) {
      context.responses(List.of());
      return this;
    }
    List<AssetOnlineItemResponse> responses = new ArrayList<>();
    for (Community community : communities) {
      if (community == null) {
        continue;
      }
      Set<AssetItem> items = onlineAssetService.listOnlineItems(community);
      if (items == null || items.isEmpty()) {
        continue;
      }
      for (AssetItem item : items) {
        AssetOnlineItemResponse response = AssetOnlineItemResponse.of(item);
        if (response != null) {
          responses.add(response);
        }
      }
    }
    context.responses(responses);
    return this;
  }

  public List<AssetOnlineItemResponse> end() {
    return context.responses();
  }
}
