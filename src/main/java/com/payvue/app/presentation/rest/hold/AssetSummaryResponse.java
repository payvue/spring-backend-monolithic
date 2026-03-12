package com.payvue.app.presentation.rest.hold;

import com.payvue.app.domain.summary.BalancesByCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record AssetSummaryResponse(
    @Schema(description = "총 캐시", example = "12500000")
    long total,
    @Schema(description = "총 평가금액", example = "12500000")
    long totalValue,
    @Schema(description = "총 자산 가치", example = "12300000")
    long totalAssetValue,
    @Schema(description = "개인 캐시", example = "8500000")
    long personal,
    @Schema(description = "개인 평가금액", example = "8500000")
    long personalValue,
    @Schema(description = "개인 자산 가치", example = "8300000")
    long personalAssetValue,
    @Schema(description = "공동 캐시", example = "4000000")
    long shared,
    @Schema(description = "공동 평가금액", example = "4000000")
    long sharedValue,
    @Schema(description = "공동 자산 가치", example = "4000000")
    long sharedAssetValue,
    @Schema(description = "월간 변화율", example = "2.4")
    double monthlyChangeRate,
    @Schema(description = "변화율", example = "5.1")
    double changeRate,
    @Schema(description = "월간 변화", example = "290000")
    long monthlyChange,
    @Schema(description = "통화 코드", example = "KRW")
    String currency,
    @Schema(description = "카테고리 분포")
    List<Category> categories
) {

  public record Category(
      @Schema(description = "카테고리", example = "game")
      String category,
      @Schema(description = "금액", example = "6000000")
      long amount,
      @Schema(description = "비율", example = "48")
      int ratio
  ) {}

  public static AssetSummaryResponse from(BalancesByCategory summary) {
    if (summary == null) {
      return new AssetSummaryResponse(
          0L,
          0L,
          0L,
          0L,
          0L,
          0L,
          0L,
          0L,
          0L,
          0.0,
          0.0,
          0L,
          "KRW",
          List.of()
      );
    }
    BalancesByCategory.Totals total = summary.totals();
    BalancesByCategory.Segment personal = summary.personal();
    BalancesByCategory.Segment shared = summary.shared();
    BalancesByCategory.Trend trend = summary.trend();
    List<Category> categories = summary.categories() == null
        ? List.of()
        : summary.categories().stream()
            .map(item -> new Category(item.category(), item.amount(), item.ratio()))
            .toList();
    return new AssetSummaryResponse(
        total == null ? 0L : total.cash(),
        total == null ? 0L : total.value(),
        total == null ? 0L : total.assetValue(),
        personal == null ? 0L : personal.cash(),
        personal == null ? 0L : personal.value(),
        personal == null ? 0L : personal.assetValue(),
        shared == null ? 0L : shared.cash(),
        shared == null ? 0L : shared.value(),
        shared == null ? 0L : shared.assetValue(),
        trend == null ? 0.0 : trend.monthlyChangeRate(),
        trend == null ? 0.0 : trend.changeRate(),
        trend == null ? 0L : trend.monthlyChange(),
        summary.currency(),
        categories
    );
  }
}
