package com.payvue.app.domain.summary;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public record BalancesByCategory(
    Totals totals,
    Segment personal,
    Segment shared,
    Trend trend,
    String currency,
    List<Category> categories
) implements SummaryView {

  public record Totals(long cash, long value, long assetValue) implements SummaryView.Totals {}

  public record Segment(long cash, long value, long assetValue) implements SummaryView.Segment {}

  public record Trend(double monthlyChangeRate, double changeRate, long monthlyChange)
      implements SummaryView.Trend {}

  public record Category(String category, long amount, int ratio)
      implements SummaryView.Category {}

  public static BalancesByCategory empty() {
    return new BalancesByCategory(
        new Totals(0L, 0L, 0L),
        new Segment(0L, 0L, 0L),
        new Segment(0L, 0L, 0L),
        new Trend(0.0, 0.0, 0L),
        "KRW",
        List.of()
    );
  }

  public static BalancesByCategory of(
      boolean personal,
      long cash,
      long assetValue,
      Map<String, Long> categoryTotals,
      String currency
  ) {
    java.util.Objects.requireNonNull(categoryTotals, "categoryTotals는 필수입니다");
    List<Category> breakdown = toCategoryBreakdown(categoryTotals, assetValue);
    Totals totals = new Totals(cash, cash, assetValue);
    Segment personalSegment = personal
        ? new Segment(cash, cash, assetValue)
        : new Segment(0L, 0L, 0L);
    Segment sharedSegment = personal
        ? new Segment(0L, 0L, 0L)
        : new Segment(cash, cash, assetValue);
    return new BalancesByCategory(
        totals,
        personalSegment,
        sharedSegment,
        new Trend(0.0, 0.0, 0L),
        currency == null || currency.isBlank() ? "KRW" : currency,
        breakdown
    );
  }

  public static BalancesByCategory ofAggregate(
      long totalCash,
      long totalValue,
      long totalAssetValue,
      long personalCash,
      long personalValue,
      long personalAssetValue,
      long sharedCash,
      long sharedValue,
      long sharedAssetValue,
      Map<String, Long> categoryTotals,
      String currency
  ) {
    java.util.Objects.requireNonNull(categoryTotals, "categoryTotals는 필수입니다");
    List<Category> breakdown = toCategoryBreakdown(categoryTotals, totalAssetValue);
    return new BalancesByCategory(
        new Totals(totalCash, totalValue, totalAssetValue),
        new Segment(personalCash, personalValue, personalAssetValue),
        new Segment(sharedCash, sharedValue, sharedAssetValue),
        new Trend(0.0, 0.0, 0L),
        currency == null || currency.isBlank() ? "KRW" : currency,
        breakdown
    );
  }

  private static List<Category> toCategoryBreakdown(
      Map<String, Long> categoryTotals,
      long totalAssetValue
  ) {
    if (categoryTotals == null || categoryTotals.isEmpty()) {
      return List.of();
    }
    List<Map.Entry<String, Long>> entries = new ArrayList<>(categoryTotals.entrySet());
    entries.sort(Comparator.comparing(Map.Entry::getKey));
    List<Category> results = new ArrayList<>(entries.size());
    for (Map.Entry<String, Long> entry : entries) {
      long amount = entry.getValue() == null ? 0L : entry.getValue();
      int ratio = totalAssetValue > 0
          ? (int) ((amount * 100L) / totalAssetValue)
          : 0;
      results.add(new Category(entry.getKey(), amount, ratio));
    }
    return results;
  }
}
