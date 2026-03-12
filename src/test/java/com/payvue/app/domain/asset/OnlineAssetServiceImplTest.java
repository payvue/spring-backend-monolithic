package com.payvue.app.domain.asset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.payvue.app.domain.asset.category.Cash;
import com.payvue.app.domain.asset.category.Mileage;
import com.payvue.core.asset.AssetClassification;
import com.payvue.core.asset.AssetItem;
import com.payvue.core.asset.AssetItemId;
import com.payvue.core.asset.AssetStatus;
import com.payvue.core.community.Community;
import com.payvue.core.community.RoleDescriptor;
import com.payvue.core.hold.Holdings;
import com.payvue.core.hold.HoldingsRepository;
import com.payvue.supporting.user.command.UserId;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("OnlineAssetServiceImpl")
class OnlineAssetServiceImplTest {

  @Test
  @DisplayName("활성 자산만 조회한다")
  void listsOnlyActiveAssets() {
    HoldingsRepository repository = mock(HoldingsRepository.class);
    OnlineAssetServiceImpl service = new OnlineAssetServiceImpl(repository);
    TestCommunity community = new TestCommunity();

    AssetItem activeCash = Cash.reconstitute(
        AssetItemId.generate(),
        "cash",
        community.getId(),
        UserId.generate(),
        AssetStatus.ACTIVE
    );
    AssetItem expiredCash = Cash.reconstitute(
        AssetItemId.generate(),
        "cash",
        community.getId(),
        UserId.generate(),
        AssetStatus.EXPIRED
    );
    AssetItem activeMileage = Mileage.reconstitute(
        AssetItemId.generate(),
        "mileage",
        community.getId(),
        UserId.generate(),
        AssetStatus.ACTIVE
    );
    Holdings holdings = Holdings.create(community, List.of(activeCash, expiredCash, activeMileage));
    when(repository.findByCommunity(community)).thenReturn(holdings);

    Set<AssetItem> result = service.listOnlineItems(community);

    assertThat(result).containsExactlyInAnyOrder(activeCash, activeMileage);
  }

  @Test
  @DisplayName("카테고리와 상태로 필터링한다")
  void filtersByCategoryAndStatus() {
    HoldingsRepository repository = mock(HoldingsRepository.class);
    OnlineAssetServiceImpl service = new OnlineAssetServiceImpl(repository);
    TestCommunity community = new TestCommunity();

    AssetItem activeCash = Cash.reconstitute(
        AssetItemId.generate(),
        "cash",
        community.getId(),
        UserId.generate(),
        AssetStatus.ACTIVE
    );
    AssetItem expiredCash = Cash.reconstitute(
        AssetItemId.generate(),
        "cash",
        community.getId(),
        UserId.generate(),
        AssetStatus.EXPIRED
    );
    AssetItem activeMileage = Mileage.reconstitute(
        AssetItemId.generate(),
        "mileage",
        community.getId(),
        UserId.generate(),
        AssetStatus.ACTIVE
    );
    Holdings holdings = Holdings.create(community, List.of(activeCash, expiredCash, activeMileage));
    when(repository.findByCommunity(community)).thenReturn(holdings);

    Set<AssetItem> result = service.listOnlineItems(
        community,
        AssetClassification.cash(),
        AssetStatus.EXPIRED
    );

    assertThat(result).containsExactly(expiredCash);
  }

  @Test
  @DisplayName("상태로만 필터링한다")
  void filtersByStatus() {
    HoldingsRepository repository = mock(HoldingsRepository.class);
    OnlineAssetServiceImpl service = new OnlineAssetServiceImpl(repository);
    TestCommunity community = new TestCommunity();

    AssetItem activeCash = Cash.reconstitute(
        AssetItemId.generate(),
        "cash",
        community.getId(),
        UserId.generate(),
        AssetStatus.ACTIVE
    );
    AssetItem expiredCash = Cash.reconstitute(
        AssetItemId.generate(),
        "cash",
        community.getId(),
        UserId.generate(),
        AssetStatus.EXPIRED
    );
    Holdings holdings = Holdings.create(community, List.of(activeCash, expiredCash));
    when(repository.findByCommunity(community)).thenReturn(holdings);

    Set<AssetItem> result = service.listOnlineItems(community, AssetStatus.EXPIRED);

    assertThat(result).containsExactly(expiredCash);
  }

  static final class TestCommunity extends Community {
    private static final RoleDescriptor ADMIN_ROLE = new TestRoleDescriptor();

    TestCommunity() {
      super("test-community", "test", UUID.randomUUID(), ADMIN_ROLE);
    }

    @Override
    public String getCommunityType() {
      return "TEST";
    }
  }

  static final class TestRoleDescriptor implements RoleDescriptor {
    @Override
    public String getName() {
      return "ADMIN";
    }

    @Override
    public boolean isAdmin() {
      return true;
    }
  }
}
