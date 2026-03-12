package com.payvue.core.asset.characteristic;

import static org.assertj.core.api.Assertions.assertThat;

import com.payvue.characteristic.HasBalance;
import com.payvue.core.asset.AssetClassification;
import com.payvue.core.asset.AssetItem;
import com.payvue.core.asset.AssetItemId;
import com.payvue.core.asset.AssetStatus;
import com.payvue.core.asset.characteristic.command.PayableCommand;
import com.payvue.core.community.CommunityId;
import com.payvue.shared.Money;
import com.payvue.supporting.user.command.UserId;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("PayablePolicy")
class PayablePolicyTest {

  @Test
  @DisplayName("잔액이 충분하면 검증이 통과된다")
  void validatesWhenBalanceIsEnough() {
    PayablePolicy policy = new PayablePolicy();
    HasBalance balance = HasBalance.of(Money.of(1000L, "KRW"));
    AssetItem item = TestAssetItem.create(balance);
    PayableCommand command = new PayableCommand.Pay(Money.of(500L, "KRW"));

    assertThat(policy.validate(item, balance, command)).isTrue();
  }

  @Test
  @DisplayName("잔액이 부족하면 검증이 실패한다")
  void failsWhenBalanceIsInsufficient() {
    PayablePolicy policy = new PayablePolicy();
    HasBalance balance = HasBalance.of(Money.of(300L, "KRW"));
    AssetItem item = TestAssetItem.create(balance);
    PayableCommand command = new PayableCommand.Pay(Money.of(500L, "KRW"));

    assertThat(policy.validate(item, balance, command)).isFalse();
  }

  @Test
  @DisplayName("음수 금액은 검증에 실패한다")
  void failsWhenAmountIsNegative() {
    PayablePolicy policy = new PayablePolicy();
    HasBalance balance = HasBalance.of(Money.of(1000L, "KRW"));
    AssetItem item = TestAssetItem.create(balance);
    PayableCommand command = new PayableCommand.Pay(Money.of(-100L, "KRW"));

    assertThat(policy.validate(item, balance, command)).isFalse();
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
