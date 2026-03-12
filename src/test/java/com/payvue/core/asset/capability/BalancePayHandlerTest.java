package com.payvue.core.asset.characteristic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

@DisplayName("BalancePayHandler")
class BalancePayHandlerTest {

  @Test
  @DisplayName("정책 통과 시 잔액이 차감된다")
  void deductsBalanceWhenPolicyPasses() {
    BalancePayHandler handler = new BalancePayHandler(new PayablePolicy());
    HasBalance balance = HasBalance.of(Money.of(1000L, "KRW"));
    AssetItem item = TestAssetItem.create(balance);
    PayableCommand command = new PayableCommand.Pay(Money.of(300L, "KRW"));

    CapabilityResult result = handler.apply(item, balance, command);

    assertThat(result.isSuccess()).isTrue();
    assertThat(((Money) result.getPayload()).getAmount().longValue()).isEqualTo(700L);
  }

  @Test
  @DisplayName("차감 금액이 잔액과 동일하면 허용된다")
  void allowsWhenAmountEqualsBalance() {
    BalancePayHandler handler = new BalancePayHandler(new PayablePolicy());
    HasBalance balance = HasBalance.of(Money.of(500L, "KRW"));
    AssetItem item = TestAssetItem.create(balance);
    PayableCommand command = new PayableCommand.Pay(Money.of(500L, "KRW"));

    CapabilityResult result = handler.apply(item, balance, command);

    assertThat(result.isSuccess()).isTrue();
    assertThat(((Money) result.getPayload()).getAmount().longValue()).isZero();
  }

  @Test
  @DisplayName("차감 금액이 잔액을 초과하면 예외가 발생한다")
  void failsWhenAmountExceedsBalance() {
    BalancePayHandler handler = new BalancePayHandler(new PayablePolicy());
    HasBalance balance = HasBalance.of(Money.of(300L, "KRW"));
    AssetItem item = TestAssetItem.create(balance);
    PayableCommand command = new PayableCommand.Pay(Money.of(500L, "KRW"));

    assertThatThrownBy(() -> handler.apply(item, balance, command))
        .isInstanceOf(IllegalArgumentException.class);
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
