package com.payvue.app.domain.asset.operation;

import static org.assertj.core.api.Assertions.assertThat;

import com.payvue.characteristic.HasBalance;
import com.payvue.core.asset.AssetClassification;
import com.payvue.core.asset.AssetItem;
import com.payvue.core.asset.AssetItemId;
import com.payvue.core.asset.AssetStatus;
import com.payvue.core.asset.characteristic.AssetCharacteristicConfiguration;
import com.payvue.core.asset.characteristic.CapabilityManager;
import com.payvue.core.community.CommunityId;
import com.payvue.core.hold.OperationResult;
import com.payvue.shared.Money;
import com.payvue.supporting.user.command.UserId;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("DisposeBalanceOperation")
class DisposeBalanceOperationTest {

  private final CapabilityManager capabilityManager =
      new AssetCharacteristicConfiguration().capabilityManager();

  @Test
  @DisplayName("잔액과 동일한 금액 차감은 성공한다")
  void deductsWhenAmountEqualsBalance() {
    AssetItem asset = TestAssetItem.create(HasBalance.of(Money.of(500L, "KRW")));
    DisposeBalanceOperation operation =
        new DisposeBalanceOperation(Money.of(500L, "KRW"), capabilityManager);

    OperationResult result = operation.execute(asset);

    assertThat(result.isSuccess()).isTrue();
    assertThat(((HasBalance) asset.getCapability(HasBalance.class).orElseThrow())
        .balance().getAmount().longValue()).isZero();
  }

  @Test
  @DisplayName("차감 금액이 0이면 잔액이 유지된다")
  void keepsBalanceWhenAmountIsZero() {
    AssetItem asset = TestAssetItem.create(HasBalance.of(Money.of(300L, "KRW")));
    DisposeBalanceOperation operation =
        new DisposeBalanceOperation(Money.of(0L, "KRW"), capabilityManager);

    OperationResult result = operation.execute(asset);

    assertThat(result.isSuccess()).isTrue();
    assertThat(((HasBalance) asset.getCapability(HasBalance.class).orElseThrow())
        .balance().getAmount().longValue()).isEqualTo(300L);
  }

  @Test
  @DisplayName("잔액이 부족하면 실패한다")
  void failsWhenBalanceInsufficient() {
    AssetItem asset = TestAssetItem.create(HasBalance.of(Money.of(200L, "KRW")));
    DisposeBalanceOperation operation =
        new DisposeBalanceOperation(Money.of(500L, "KRW"), capabilityManager);

    OperationResult result = operation.execute(asset);

    assertThat(result.isFailure()).isTrue();
  }

  static final class TestAssetItem extends AssetItem {
    private TestAssetItem(AssetItemId id, CommunityId communityId, UserId contributorId) {
      super(
          id,
          "asset",
          communityId,
          contributorId,
          AssetClassification.cash(),
          Set.of(),
          AssetStatus.ACTIVE
      );
    }

    static TestAssetItem create(HasBalance balance) {
      TestAssetItem item = new TestAssetItem(
          AssetItemId.generate(),
          CommunityId.generate(),
          UserId.generate()
      );
      item.with(balance);
      return item;
    }
  }
}
