package com.payvue.app.domain.summary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("BalancesCategoryingAggregationStrategy")
class BalancesCategoryingAggregationStrategyTest {

  @Test
  @DisplayName("합산 결과를 누적한다")
  void aggregatesSummaries() {
    BalancesByCategory first = BalancesByCategory.of(
        true,
        100L,
        100L,
        Map.of("CASH", 100L),
        "KRW"
    );
    BalancesByCategory second = BalancesByCategory.of(
        false,
        200L,
        200L,
        Map.of("GIFT", 200L),
        "KRW"
    );

    BalancesByCategory result = new BalancesCategoryingAggregationStrategy()
        .aggregate(List.of(first, second));

    assertThat(result.totals().cash()).isEqualTo(300L);
    assertThat(result.totals().assetValue()).isEqualTo(300L);
    assertThat(result.categories()).hasSize(2);
  }

  @Test
  @DisplayName("통화는 첫 번째 유효 값을 선택한다")
  void selectsFirstNonBlankCurrency() {
    BalancesByCategory first = BalancesByCategory.ofAggregate(
        100L,
        100L,
        100L,
        100L,
        100L,
        100L,
        0L,
        0L,
        0L,
        Map.of("CASH", 100L),
        "KRW"
    );
    BalancesByCategory second = BalancesByCategory.ofAggregate(
        50L,
        50L,
        50L,
        0L,
        0L,
        0L,
        50L,
        50L,
        50L,
        Map.of("GIFT", 50L),
        "USD"
    );

    BalancesByCategory result = new BalancesCategoryingAggregationStrategy()
        .aggregate(List.of(first, second));

    assertThat(result.currency()).isEqualTo("KRW");
  }
}
