package com.payvue.app.domain.asset.operation;

import static org.assertj.core.api.Assertions.assertThat;

import com.payvue.characteristic.Consumable;
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
import com.payvue.supporting.user.command.UserId;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AcquireConsumableOperation")
class AcquireConsumableOperationTest {

  private final CapabilityManager capabilityManager =
      new AssetCharacteristicConfiguration().capabilityManager();

  @Test
  @DisplayName("새 자산이면 holdings에 추가된다")
  void addsNewAssetToHoldings() {
    TestCommunity community = new TestCommunity();
    Holdings holdings = Holdings.create(community);
    AssetItem asset = TestAssetItem.create(community.getId(), "coupon", Consumable.singleUse(1));

    AcquireConsumableOperation operation = new AcquireConsumableOperation(holdings, capabilityManager);

    OperationResult result = operation.execute(asset);

    assertThat(result.isSuccess()).isTrue();
    assertThat(holdings.getAssets()).contains(asset);
  }

  @Test
  @DisplayName("동일 자산이면 수량이 합산된다")
  void mergesQuantityWhenSameAssetExists() {
    TestCommunity community = new TestCommunity();
    AssetItem existing = TestAssetItem.create(community.getId(), "coupon", Consumable.singleUse(2));
    Holdings holdings = Holdings.create(community, List.of(existing));

    AssetItem incoming = TestAssetItem.create(community.getId(), "coupon", Consumable.singleUse(3));
    AcquireConsumableOperation operation = new AcquireConsumableOperation(holdings, capabilityManager);

    OperationResult result = operation.execute(incoming);

    assertThat(result.isSuccess()).isTrue();
    Consumable consumable = existing.getCapability(Consumable.class).orElseThrow();
    assertThat(consumable.quantity()).isEqualTo(5);
  }

  @Test
  @DisplayName("소모성 능력이 없으면 실패한다")
  void failsWhenConsumableMissing() {
    TestCommunity community = new TestCommunity();
    AssetItem existing = TestAssetItem.create(community.getId(), "coupon", null);
    Holdings holdings = Holdings.create(community, List.of(existing));

    AssetItem incoming = TestAssetItem.create(community.getId(), "coupon", null);
    AcquireConsumableOperation operation = new AcquireConsumableOperation(holdings, capabilityManager);

    OperationResult result = operation.execute(incoming);

    assertThat(result.isFailure()).isTrue();
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

    static TestAssetItem create(CommunityId communityId, String name, Consumable consumable) {
      TestAssetItem item = new TestAssetItem(
          AssetItemId.generate(),
          name,
          communityId,
          UserId.generate()
      );
      if (consumable != null) {
        item.with(consumable);
      }
      return item;
    }
  }
}
