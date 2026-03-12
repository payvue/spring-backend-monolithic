package com.payvue.core.transaction.command;

import static org.assertj.core.api.Assertions.assertThat;

import com.payvue.characteristic.HasBalance;
import com.payvue.common.status.ExecutionState;
import com.payvue.core.asset.AssetClassification;
import com.payvue.core.asset.AssetItem;
import com.payvue.core.asset.AssetItemId;
import com.payvue.core.asset.AssetStatus;
import com.payvue.core.community.Community;
import com.payvue.core.community.CommunityId;
import com.payvue.core.community.RoleDescriptor;
import com.payvue.core.transaction.command.meta.TransactionMeta;
import com.payvue.core.transaction.command.meta.TransactionMetaKey;
import com.payvue.shared.Money;
import com.payvue.supporting.user.command.UserId;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CommissionTransactionDecorator")
class CommissionTransactionDecoratorTest {

  @Test
  @DisplayName("잔액에 RATE를 곱한 수수료를 메타에 기록한다")
  void recordsCommissionMeta() {
    TestCommunity community = new TestCommunity();
    UserId actorId = UserId.generate();
    AssetItem item = TestAssetItem.create(community.getId(), actorId);

    Transaction transaction = Transaction.reconstitute(
        TransactionId.generate(),
        TransactionType.PAYMENT,
        AssetType.CASH.name(),
        community,
        community,
        item,
        1000L,
        actorId,
        "description",
        item.getCategory().getValue(),
        ExecutionState.COMPLETED,
        Instant.now(),
        TransactionMeta.empty()
    );

    CommissionTransactionDecorator decorator = new CommissionTransactionDecorator();

    Transaction decorated = decorator.decorate(transaction);

    assertThat(decorated.meta().get(TransactionMetaKey.COMMISSION)).isEqualTo("500.00");
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
        UserId contributorId,
        AssetClassification category,
        Set<com.payvue.characteristic.AssetCharacteristic> capabilities,
        AssetStatus status
    ) {
      super(id, name, communityId, contributorId, category, capabilities, status);
    }

    static TestAssetItem create(CommunityId communityId, UserId contributorId) {
      return new TestAssetItem(
          AssetItemId.generate(),
          "asset-item",
          communityId,
          contributorId,
          AssetClassification.cash(),
          Set.of(HasBalance.of(Money.of(1000L, "KRW"))),
          AssetStatus.ACTIVE
      );
    }
  }
}
