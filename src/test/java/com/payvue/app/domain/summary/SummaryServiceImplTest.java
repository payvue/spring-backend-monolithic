package com.payvue.app.domain.summary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.payvue.app.domain.asset.category.Cash;
import com.payvue.app.domain.asset.category.GiftCard;
import com.payvue.app.domain.community.Cooperative;
import com.payvue.app.domain.community.Personal;
import com.payvue.app.domain.community.Role;
import com.payvue.characteristic.HasBalance;
import com.payvue.core.asset.AssetClassification;
import com.payvue.core.asset.AssetItem;
import com.payvue.core.hold.Holdings;
import com.payvue.core.hold.HoldingsRepository;
import com.payvue.shared.Money;
import com.payvue.supporting.user.command.UserId;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SummaryServiceImpl")
class SummaryServiceImplTest {

  @Test
  @DisplayName("summarize(community, categories)는 카테고리별 합산을 포함한다")
  void summarizesCommunityByCategory() {
    HoldingsRepository repository = mock(HoldingsRepository.class);
    SummaryAggregationStrategy<BalancesByCategory> aggregation = new BalancesCategoryingAggregationStrategy();
    SummaryServiceImpl service = new SummaryServiceImpl(repository, aggregation);
    Personal community = personalCommunity();

    AssetItem cash = Cash.create(community.getId(), UserId.generate(), "cash")
        .with(HasBalance.of(Money.of(1000L, "KRW")));
    AssetItem gift = GiftCard.create(community.getId(), UserId.generate(), "gift")
        .with(HasBalance.of(Money.of(500L, "KRW")));
    Holdings holdings = Holdings.create(community, List.of(cash, gift));
    when(repository.findByCommunity(community)).thenReturn(holdings);

    BalancesByCategory summary = service.summarize(community, Set.of(AssetClassification.gift()));

    assertThat(summary.totals().cash()).isEqualTo(1000L);
    assertThat(summary.totals().assetValue()).isEqualTo(1500L);
    assertThat(summary.personal().assetValue()).isEqualTo(1500L);
    assertThat(summary.categories()).hasSize(1);
    assertThat(summary.categories().get(0).category()).isEqualTo(AssetClassification.gift().getValue());
  }

  @Test
  @DisplayName("summarize(community, categories)는 빈 카테고리면 기본 카테고리를 사용한다")
  void summarizesWithDefaultCategoriesWhenEmptySet() {
    HoldingsRepository repository = mock(HoldingsRepository.class);
    SummaryAggregationStrategy<BalancesByCategory> aggregation = new BalancesCategoryingAggregationStrategy();
    SummaryServiceImpl service = new SummaryServiceImpl(repository, aggregation);
    Personal community = personalCommunity();

    AssetItem gift = GiftCard.create(community.getId(), UserId.generate(), "gift")
        .with(HasBalance.of(Money.of(400L, "KRW")));
    Holdings holdings = Holdings.create(community, List.of(gift));
    when(repository.findByCommunity(community)).thenReturn(holdings);

    BalancesByCategory summary = service.summarize(community, Set.of());

    assertThat(summary.categories()).hasSize(1);
    assertThat(summary.categories().get(0).category()).isEqualTo(AssetClassification.gift().getValue());
  }

  @Test
  @DisplayName("summarize(community, category)는 shared 세그먼트로 집계된다")
  void summarizesSharedCommunity() {
    HoldingsRepository repository = mock(HoldingsRepository.class);
    SummaryAggregationStrategy<BalancesByCategory> aggregation = new BalancesCategoryingAggregationStrategy();
    SummaryServiceImpl service = new SummaryServiceImpl(repository, aggregation);
    Cooperative community = organizationCommunity();

    AssetItem cash = Cash.create(community.getId(), UserId.generate(), "cash")
        .with(HasBalance.of(Money.of(700L, "KRW")));
    Holdings holdings = Holdings.create(community, List.of(cash));
    when(repository.findByCommunity(community)).thenReturn(holdings);

    BalancesByCategory summary = service.summarize(community, AssetClassification.cash());

    assertThat(summary.shared().cash()).isEqualTo(700L);
    assertThat(summary.personal().cash()).isZero();
  }

  @Test
  @DisplayName("summarize(communities, categories)는 빈 입력이면 empty를 반환한다")
  void summarizesEmptyCommunities() {
    HoldingsRepository repository = mock(HoldingsRepository.class);
    SummaryAggregationStrategy<BalancesByCategory> aggregation = new BalancesCategoryingAggregationStrategy();
    SummaryServiceImpl service = new SummaryServiceImpl(repository, aggregation);

    BalancesByCategory result = service.summarize(Set.of(), Set.of(AssetClassification.cash()));

    assertThat(result).isEqualTo(BalancesByCategory.empty());
  }

  @Test
  @DisplayName("summarize(communities, categories)는 다중 커뮤니티를 합산한다")
  void summarizesMultipleCommunities() {
    HoldingsRepository repository = mock(HoldingsRepository.class);
    SummaryAggregationStrategy<BalancesByCategory> aggregation = new BalancesCategoryingAggregationStrategy();
    SummaryServiceImpl service = new SummaryServiceImpl(repository, aggregation);
    Personal personal = personalCommunity();
    Cooperative shared = organizationCommunity();

    AssetItem personalCash = Cash.create(personal.getId(), UserId.generate(), "cash")
        .with(HasBalance.of(Money.of(300L, "KRW")));
    Holdings personalHoldings = Holdings.create(personal, List.of(personalCash));
    when(repository.findByCommunity(personal)).thenReturn(personalHoldings);

    AssetItem sharedCash = Cash.create(shared.getId(), UserId.generate(), "cash")
        .with(HasBalance.of(Money.of(700L, "KRW")));
    Holdings sharedHoldings = Holdings.create(shared, List.of(sharedCash));
    when(repository.findByCommunity(shared)).thenReturn(sharedHoldings);

    BalancesByCategory result = service.summarize(
        Set.of(personal, shared),
        Set.of(AssetClassification.cash())
    );

    assertThat(result.totals().cash()).isEqualTo(1000L);
    assertThat(result.personal().cash()).isEqualTo(300L);
    assertThat(result.shared().cash()).isEqualTo(700L);
  }

  @Test
  @DisplayName("summarize(communities, categories)는 빈 카테고리면 기본 카테고리를 사용한다")
  void summarizesCommunitiesWithDefaultCategories() {
    HoldingsRepository repository = mock(HoldingsRepository.class);
    SummaryAggregationStrategy<BalancesByCategory> aggregation = new BalancesCategoryingAggregationStrategy();
    SummaryServiceImpl service = new SummaryServiceImpl(repository, aggregation);
    Personal personal = personalCommunity();

    AssetItem gift = GiftCard.create(personal.getId(), UserId.generate(), "gift")
        .with(HasBalance.of(Money.of(250L, "KRW")));
    Holdings holdings = Holdings.create(personal, List.of(gift));
    when(repository.findByCommunity(personal)).thenReturn(holdings);

    BalancesByCategory result = service.summarize(Set.of(personal), Set.of());

    assertThat(result.categories()).hasSize(1);
    assertThat(result.categories().get(0).category()).isEqualTo(AssetClassification.gift().getValue());
  }

  private Personal personalCommunity() {
    return Personal.create(
        "personal",
        "personal community",
        UUID.randomUUID(),
        Role.ADMIN
    );
  }

  private Cooperative organizationCommunity() {
    return Cooperative.create(
        "organization",
        "organization community",
        UUID.randomUUID(),
        Role.ADMIN,
        Role.MEMBER
    );
  }
}
