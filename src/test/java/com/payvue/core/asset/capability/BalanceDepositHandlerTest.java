package com.payvue.core.asset.characteristic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.payvue.characteristic.HasBalance;
import com.payvue.core.asset.AssetClassification;
import com.payvue.core.asset.AssetItem;
import com.payvue.core.asset.AssetItemId;
import com.payvue.core.asset.AssetStatus;
import com.payvue.core.asset.characteristic.command.DepositCommand;
import com.payvue.core.community.CommunityId;
import com.payvue.shared.Money;
import com.payvue.supporting.user.command.UserId;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("BalanceDepositHandler")
class BalanceDepositHandlerTest {

  @Test
  @DisplayName("정책 통과 시 잔액이 증가한다")
  void addsBalanceWhenPolicyPasses() {
    BalanceDepositHandler handler = new BalanceDepositHandler(new DepositPolicy());
    HasBalance balance = HasBalance.of(Money.of(500L, "KRW"));
    AssetItem item = TestAssetItem.create(balance);
    DepositCommand command = new DepositCommand.Deposit(Money.of(200L, "KRW"));

    CapabilityResult result = handler.apply(item, balance, command);

    assertThat(result.isSuccess()).isTrue();
    assertThat(((Money) result.getPayload()).getAmount().longValue()).isEqualTo(700L);
  }

  @Test
  @DisplayName("0원 입금은 허용된다")
  void allowsZeroAmountDeposit() {
    BalanceDepositHandler handler = new BalanceDepositHandler(new DepositPolicy());
    HasBalance balance = HasBalance.of(Money.of(500L, "KRW"));
    AssetItem item = TestAssetItem.create(balance);
    DepositCommand command = new DepositCommand.Deposit(Money.of(0L, "KRW"));

    CapabilityResult result = handler.apply(item, balance, command);

    assertThat(result.isSuccess()).isTrue();
    assertThat(((Money) result.getPayload()).getAmount().longValue()).isEqualTo(500L);
  }

  @Test
  @DisplayName("음수 금액 입금은 예외가 발생한다")
  void failsWhenAmountIsNegative() {
    BalanceDepositHandler handler = new BalanceDepositHandler(new DepositPolicy());
    HasBalance balance = HasBalance.of(Money.of(500L, "KRW"));
    AssetItem item = TestAssetItem.create(balance);
    DepositCommand command = new DepositCommand.Deposit(Money.of(-100L, "KRW"));

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
