package com.payvue.app.domain.summary;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class BalancesCategoryingAggregationStrategy
    implements SummaryAggregationStrategy<BalancesByCategory> {

  @Override
  public BalancesByCategory aggregate(Collection<BalancesByCategory> summaries) {
    java.util.Objects.requireNonNull(summaries, "summaries는 필수입니다");
    if (summaries.isEmpty()) {
      return BalancesByCategory.empty();
    }
    long totalCash = 0L;
    long totalValue = 0L;
    long totalAssetValue = 0L;
    long personalCash = 0L;
    long personalValue = 0L;
    long personalAssetValue = 0L;
    long sharedCash = 0L;
    long sharedValue = 0L;
    long sharedAssetValue = 0L;
    Map<String, Long> categoryTotals = new HashMap<>();
    String currency = null;

    for (BalancesByCategory summary : summaries) {
      java.util.Objects.requireNonNull(summary, "summary는 필수입니다");
      if (currency == null && summary.currency() != null && !summary.currency().isBlank()) {
        currency = summary.currency();
      }
      BalancesByCategory.Totals totals = summary.totals();
      java.util.Objects.requireNonNull(totals, "totals는 필수입니다");
      totalCash += totals.cash();
      totalValue += totals.value();
      totalAssetValue += totals.assetValue();
      BalancesByCategory.Segment personal = summary.personal();
      java.util.Objects.requireNonNull(personal, "personal는 필수입니다");
      personalCash += personal.cash();
      personalValue += personal.value();
      personalAssetValue += personal.assetValue();
      BalancesByCategory.Segment shared = summary.shared();
      java.util.Objects.requireNonNull(shared, "shared는 필수입니다");
      sharedCash += shared.cash();
      sharedValue += shared.value();
      sharedAssetValue += shared.assetValue();
      java.util.Objects.requireNonNull(summary.categories(), "categories는 필수입니다");
      for (BalancesByCategory.Category category : summary.categories()) {
        java.util.Objects.requireNonNull(category, "category는 필수입니다");
        java.util.Objects.requireNonNull(category.category(), "categoryName은 필수입니다");
        categoryTotals.merge(category.category(), category.amount(), Long::sum);
      }
    }

    return BalancesByCategory.ofAggregate(
        totalCash,
        totalValue,
        totalAssetValue,
        personalCash,
        personalValue,
        personalAssetValue,
        sharedCash,
        sharedValue,
        sharedAssetValue,
        categoryTotals,
        currency
    );
  }
}
