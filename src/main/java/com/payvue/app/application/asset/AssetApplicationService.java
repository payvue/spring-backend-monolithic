package com.payvue.app.application.asset;

import com.payvue.app.application.asset.create.OnlineAssetCreateContext;
import com.payvue.app.application.asset.create.OnlineAssetCreateFlow;
import com.payvue.app.application.asset.online.OnlineAssetFlow;
import com.payvue.app.application.asset.method.PaymentMethodFlow;
import com.payvue.app.domain.asset.OnlineAssetService;
import com.payvue.core.community.CommunityContextManager;
import com.payvue.app.presentation.rest.hold.AssetOnlineItemResponse;
import com.payvue.app.presentation.rest.hold.AssetOnlineCreateRequest;
import com.payvue.app.presentation.rest.hold.PaymentMethodResponse;
import com.payvue.core.community.CommunityId;
import com.payvue.core.hold.HoldingsRepository;
import com.payvue.supporting.user.UserContextManager;

import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AssetApplicationService {

  private final CommunityContextManager communityContextManager;
  private final OnlineAssetService onlineAssetService;
  private final UserContextManager userContextManager;
  private final HoldingsRepository holdingsRepository;

  public List<AssetOnlineItemResponse> online(UserDetails userDetails) {
    Objects.requireNonNull(userDetails, "userDetails는 필수입니다.");
    return OnlineAssetFlow.start(userDetails)
        .user(step -> step.loadUser(userContextManager))
        .community(step -> step.loadPersonalCommunity(communityContextManager))
        .asset(step -> step.loadOnlineAssets(onlineAssetService))
        .end();
  }

  public List<AssetOnlineItemResponse> online(
      UserDetails userDetails,
      String category
  ) {
    Objects.requireNonNull(userDetails, "userDetails는 필수입니다.");
    Objects.requireNonNull(category, "category는 필수입니다.");
    if (category.isBlank()) {
      throw new IllegalArgumentException("category는 비어 있을 수 없습니다.");
    }
    return OnlineAssetFlow.startWithCategory(userDetails, category)
        .user(step -> step.loadUser(userContextManager))
        .community(step -> step.loadCommunitiesByCategory(communityContextManager))
        .asset(step -> step.loadOnlineAssets(onlineAssetService))
        .end();
  }

  public List<AssetOnlineItemResponse> online(UserDetails userDetails, CommunityId id) {
    Objects.requireNonNull(userDetails, "userDetails는 필수입니다.");
    Objects.requireNonNull(id, "communityId는 필수입니다.");
    return OnlineAssetFlow.startWithCommunityId(userDetails, id)
        .user(step -> step.loadUser(userContextManager))
        .community(step -> step.loadCommunitiesById(communityContextManager))
        .asset(step -> step.loadOnlineAssets(onlineAssetService))
        .end();
  }

  public List<PaymentMethodResponse> paymentMethods(UserDetails userDetails) {
    return PaymentMethodFlow.start(userDetails)
        .user(step -> step.loadUser(userContextManager))
        .result(PaymentMethodFlow::buildResponses)
        .end();
  }

  public String createOnlineAsset(
      UserDetails userDetails,
      AssetOnlineCreateRequest request
  ) {
    Objects.requireNonNull(userDetails, "userDetails는 필수입니다.");
    Objects.requireNonNull(request, "request는 필수입니다.");
    OnlineAssetCreateContext context = OnlineAssetCreateFlow.start(userDetails, request)
        .user(step -> step.loadUser(userContextManager))
        .community(step -> step.loadPersonalCommunity(communityContextManager))
        .holdings(step -> step.loadHoldings(holdingsRepository))
        .asset(step -> step.addOnlineAsset())
        .save(step -> step.persist(holdingsRepository))
        .end();

    return context.assetId();
  }

}
