package com.payvue.app.domain.asset.operation;

import static org.assertj.core.api.Assertions.assertThat;

import com.payvue.characteristic.HasBalance;
import com.payvue.core.asset.AssetClassification;
import com.payvue.core.asset.AssetItem;
import com.payvue.core.asset.AssetItemId;
import com.payvue.core.asset.AssetStatus;
import com.payvue.core.asset.characteristic.AssetCharacteristicConfiguration;
import com.payvue.core.asset.characteristic.CapabilityManager;
import com.payvue.core.community.Community;
import com.payvue.core.community.CommunityId;
import com.payvue.core.community.RoleDescriptor;
import com.payvue.core.hold.Holdings;
import com.payvue.core.hold.OperationResult;
import com.payvue.shared.Money;
import com.payvue.supporting.user.command.UserId;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AcquireBalanceOperation")
class AcquireBalanceOperationTest {

  private final CapabilityManager capabilityManager =
      new AssetCharacteristicConfiguration().capabilityManager();

  @Test
  @DisplayName("새 자산이면 holdings에 추가된다")
  void addsNewAssetToHoldings() {
    TestCommunity community = new TestCommunity();
    Holdings holdings = Holdings.create(community);
    AssetItem asset = TestAssetItem.create(community.getId(), "cash", Money.of(1000L, "KRW"));

    AcquireBalanceOperation operation = new AcquireBalanceOperation(holdings, capabilityManager);

    OperationResult result = operation.execute(asset);

    assertThat(result.isSuccess()).isTrue();
    assertThat(holdings.getAssets()).contains(asset);
  }

  @Test
  @DisplayName("동일 자산은 잔액이 합산된다")
  void mergesBalanceWhenSameAssetExists() {
    TestCommunity community = new TestCommunity();
    AssetItem existing = TestAssetItem.create(community.getId(), "cash", Money.of(500L, "KRW"));
    Holdings holdings = Holdings.create(community, List.of(existing));

    AssetItem incoming = TestAssetItem.create(community.getId(), "cash", Money.of(300L, "KRW"));
    AcquireBalanceOperation operation = new AcquireBalanceOperation(holdings, capabilityManager);

    OperationResult result = operation.execute(incoming);

    assertThat(result.isSuccess()).isTrue();
    HasBalance balance = existing.getCapability(HasBalance.class).orElseThrow();
    assertThat(balance.balance().getAmount().longValue()).isEqualTo(800L);
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

  static final class TestAssetItem extends AssetItem {
    private TestAssetItem(
        AssetItemId id,
        String name,
        CommunityId communityId,
        UserId contributorId
    ) {
      super(
          id,
          name,
          communityId,
          contributorId,
          AssetClassification.cash(),
          Set.of(),
          AssetStatus.ACTIVE
      );
    }

    static TestAssetItem create(CommunityId communityId, String name, Money balance) {
      TestAssetItem item = new TestAssetItem(
          AssetItemId.generate(),
          name,
          communityId,
          UserId.generate()
      );
      item.with(HasBalance.of(balance));
      return item;
    }
  }
}
