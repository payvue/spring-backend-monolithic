package com.payvue.app.domain.summary;

import com.payvue.characteristic.HasBalance;
import com.payvue.core.asset.AssetClassification;
import com.payvue.core.asset.AssetItem;
import com.payvue.core.community.Community;
import com.payvue.core.hold.Holdings;
import com.payvue.core.hold.HoldingsRepository;
import com.payvue.app.domain.asset.specification.ActiveAssetSpecification;
import com.payvue.app.domain.asset.specification.HasCapabilitySpecification;
import com.payvue.shared.Money;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SummaryServiceImpl implements SummaryService<BalancesByCategory> {

  private final HoldingsRepository holdingsRepository;
  private final SummaryAggregationStrategy<BalancesByCategory> aggregationStrategy;
  private static final Set<AssetClassification> DEFAULT_CATEGORIES = Set.of(
      AssetClassification.game(),
      AssetClassification.music(),
      AssetClassification.gift(),
      AssetClassification.shopping()
  );

  @Override
  public BalancesByCategory summarize(Community community, Set<AssetClassification> categories) {
    java.util.Objects.requireNonNull(community, "community는 필수입니다");
    java.util.Objects.requireNonNull(categories, "categories는 필수입니다");
    Set<AssetClassification> allowed = normalizeCategories(categories);
    return summarizeCommunity(community, allowed);
  }

  @Override
  public BalancesByCategory summarize(Community community, AssetClassification category) {
    java.util.Objects.requireNonNull(community, "community는 필수입니다");
    java.util.Objects.requireNonNull(category, "category는 필수입니다");
    return summarize(community, java.util.Set.of(category));
  }

  @Override
  public BalancesByCategory summarize(Set<Community> communities, Set<AssetClassification> categories) {
    java.util.Objects.requireNonNull(communities, "communities는 필수입니다");
    java.util.Objects.requireNonNull(categories, "categories는 필수입니다");
    if (communities.isEmpty()) {
      return BalancesByCategory.empty();
    }
    Set<AssetClassification> allowed = normalizeCategories(categories);
    List<BalancesByCategory> summaries = new ArrayList<>();
    for (Community community : communities) {
      java.util.Objects.requireNonNull(community, "community는 필수입니다");
      summaries.add(summarizeCommunity(community, allowed));
    }
    return aggregationStrategy.aggregate(summaries);
  }

  private BalancesByCategory summarizeCommunity(
      Community community,
      Set<AssetClassification> allowedCategories
  ) {
    java.util.Objects.requireNonNull(community, "community는 필수입니다");
    java.util.Objects.requireNonNull(allowedCategories, "allowedCategories는 필수입니다");
    Holdings holdings = java.util.Objects.requireNonNull(
        holdingsRepository.findByCommunity(community),
        "holdings는 필수입니다"
    );
    boolean personal = isPersonal(community);
    long cash = 0L;
    long assetValue = 0L;
    Map<String, Long> categoryTotals = new HashMap<>();
    Set<AssetItem> activeAssets =
        holdings.ownedAssetsConformingTo(
                new ActiveAssetSpecification()
                .and(new HasCapabilitySpecification(HasBalance.class)));
    for (AssetItem item : activeAssets) {
      java.util.Objects.requireNonNull(item, "item은 필수입니다");
      java.util.Objects.requireNonNull(item.getCategory(), "category는 필수입니다");
      HasBalance balance = item.getCapability(HasBalance.class)
          .orElseThrow(() -> new IllegalStateException("HasBalance 능력 없음"));
      Money value = java.util.Objects.requireNonNull(balance.balance(), "balance는 필수입니다");
      long amount = value.getAmount().setScale(0, RoundingMode.DOWN).longValue();
      if (item.getCategory().isCash()) {
        cash += amount;
      }
      assetValue += amount;
      if (allowedCategories.isEmpty()
          || allowedCategories.contains(item.getCategory())) {
        categoryTotals.merge(item.getCategory().getValue(), amount, Long::sum);
      }
    }
    return BalancesByCategory.of(
        personal,
        cash,
        assetValue,
        categoryTotals,
        "KRW"
    );
  }

  private boolean isPersonal(Community community) {
    java.util.Objects.requireNonNull(community, "community는 필수입니다");
    java.util.Objects.requireNonNull(community.getCommunityType(), "communityType은 필수입니다");
    return "PERSONAL".equalsIgnoreCase(community.getCommunityType());
  }

  private Set<AssetClassification> normalizeCategories(Set<AssetClassification> categories) {
    java.util.Objects.requireNonNull(categories, "categories는 필수입니다");
    if (categories.isEmpty()) {
      return DEFAULT_CATEGORIES;
    }
    return new HashSet<>(categories);
  }

}
