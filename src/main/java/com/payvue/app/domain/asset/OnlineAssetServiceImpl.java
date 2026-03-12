package com.payvue.app.domain.asset;

import com.payvue.core.asset.AssetClassification;
import com.payvue.core.asset.AssetItem;
import com.payvue.core.asset.AssetStatus;
import com.payvue.core.community.Community;
import com.payvue.core.hold.HoldingSpecification;
import com.payvue.core.hold.Holdings;
import com.payvue.core.hold.HoldingsRepository;
import com.payvue.app.domain.asset.specification.ActiveAssetSpecification;
import com.payvue.app.domain.asset.specification.CategorySpecification;
import com.payvue.app.domain.asset.specification.StatusSpecification;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OnlineAssetServiceImpl implements OnlineAssetService {

  private final HoldingsRepository holdingsRepository;

  @Override
  public Set<AssetItem> listOnlineItems(Community community) {
    java.util.Objects.requireNonNull(community, "community는 필수입니다");
    return filterBySpecification(community, new ActiveAssetSpecification());
  }

  @Override
  public Set<AssetItem> listOnlineItems(Community community, AssetClassification category) {
    java.util.Objects.requireNonNull(community, "community는 필수입니다");
    java.util.Objects.requireNonNull(category, "category는 필수입니다");
    HoldingSpecification spec = new ActiveAssetSpecification()
        .and(new CategorySpecification(category));
    return filterBySpecification(community, spec);
  }

  @Override
  public Set<AssetItem> listOnlineItems(Community community, AssetStatus status) {
    java.util.Objects.requireNonNull(community, "community는 필수입니다");
    java.util.Objects.requireNonNull(status, "status는 필수입니다");
    return filterBySpecification(community, new StatusSpecification(status));
  }

  @Override
  public Set<AssetItem> listOnlineItems(
      Community community,
      AssetClassification category,
      AssetStatus status
  ) {
    java.util.Objects.requireNonNull(community, "community는 필수입니다");
    java.util.Objects.requireNonNull(category, "category는 필수입니다");
    java.util.Objects.requireNonNull(status, "status는 필수입니다");
    HoldingSpecification spec = null;
    HoldingSpecification categorySpec = new CategorySpecification(category);
    HoldingSpecification statusSpec = new StatusSpecification(status);
    spec = categorySpec.and(statusSpec);
    return filterBySpecification(community, spec);
  }

  private Set<AssetItem> filterBySpecification(
      Community community,
      HoldingSpecification specification
  ) {
    java.util.Objects.requireNonNull(community, "community는 필수입니다");
    java.util.Objects.requireNonNull(specification, "specification은 필수입니다");
    Holdings holdings = holdingsRepository.findByCommunity(community);
    return holdings.ownedAssetsConformingTo(specification);
  }
}
