package com.payvue.core.asset.characteristic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.payvue.characteristic.Acquirable;
import com.payvue.characteristic.Consumable;
import com.payvue.characteristic.HasBalance;
import com.payvue.core.asset.AssetClassification;
import com.payvue.core.asset.AssetItem;
import com.payvue.core.asset.AssetItemId;
import com.payvue.core.asset.AssetStatus;
import com.payvue.core.asset.characteristic.command.AcquirableCommand;
import com.payvue.core.asset.characteristic.command.DepositCommand;
import com.payvue.core.asset.characteristic.command.PayableCommand;
import com.payvue.core.asset.characteristic.command.QuantityCommand;
import com.payvue.core.community.CommunityId;
import com.payvue.shared.Money;
import com.payvue.supporting.user.command.UserId;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@DisplayName("CapabilityManagerIntegration")
@SpringBootTest(classes = AssetCharacteristicConfiguration.class)
class AssetCharacteristicConfigurationBootTest {

  @Autowired
  private CapabilityManager capabilityManager;

  @Test
  @DisplayName("HasBalance + PayableCommand 경로가 정상 동작한다")
  void processesPayableCommand() {
    AssetItem item = TestAssetItem.create(HasBalance.of(Money.of(1000L, "KRW")));

    CapabilityResult result = capabilityManager.process(
        item,
        HasBalance.class,
        new PayableCommand.Pay(Money.of(300L, "KRW"))
    );

    assertThat(result.isSuccess()).isTrue();
    assertThat(((Money) result.getPayload()).getAmount().longValue()).isEqualTo(700L);
  }

  @Test
  @DisplayName("HasBalance 잔액이 0이고 차감이 0이면 성공한다")
  void processesPayableCommandWithZeroBalance() {
    AssetItem item = TestAssetItem.create(HasBalance.of(Money.of(0L, "KRW")));

    CapabilityResult result = capabilityManager.process(
        item,
        HasBalance.class,
        new PayableCommand.Pay(Money.of(0L, "KRW"))
    );

    assertThat(result.isSuccess()).isTrue();
    assertThat(((Money) result.getPayload()).getAmount().longValue()).isZero();
  }

  @Test
  @DisplayName("HasBalance 잔액이 부족하면 예외가 발생한다")
  void payableCommandFailsWhenBalanceInsufficient() {
    AssetItem item = TestAssetItem.create(HasBalance.of(Money.of(0L, "KRW")));

    assertThatThrownBy(() -> capabilityManager.process(
        item,
        HasBalance.class,
        new PayableCommand.Pay(Money.of(100L, "KRW"))
    )).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("HasBalance + DepositCommand 경로가 정상 동작한다")
  void processesDepositCommand() {
    AssetItem item = TestAssetItem.create(HasBalance.of(Money.of(500L, "KRW")));

    CapabilityResult result = capabilityManager.process(
        item,
        HasBalance.class,
        new DepositCommand.Deposit(Money.of(200L, "KRW"))
    );

    assertThat(result.isSuccess()).isTrue();
    assertThat(((Money) result.getPayload()).getAmount().longValue()).isEqualTo(700L);
  }

  @Test
  @DisplayName("Deposit 0원은 잔액을 유지한다")
  void processesDepositCommandWithZeroAmount() {
    AssetItem item = TestAssetItem.create(HasBalance.of(Money.of(500L, "KRW")));

    CapabilityResult result = capabilityManager.process(
        item,
        HasBalance.class,
        new DepositCommand.Deposit(Money.of(0L, "KRW"))
    );

    assertThat(result.isSuccess()).isTrue();
    assertThat(((Money) result.getPayload()).getAmount().longValue()).isEqualTo(500L);
  }
  @Test
  @DisplayName("Consumable + QuantityCommand.Consume 경로가 정상 동작한다")
  void processesConsumeCommand() {
    AssetItem item = TestAssetItem.create(Consumable.multipleUse(5));

    CapabilityResult result = capabilityManager.process(
        item,
        Consumable.class,
        new QuantityCommand.Consume(2)
    );

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getPayload()).isEqualTo(3);
  }

  @Test
  @DisplayName("Consumable 수량이 0이고 Consume이 0이면 성공한다")
  void processesConsumeCommandWithZeroQuantity() {
    AssetItem item = TestAssetItem.create(Consumable.multipleUse(0));

    CapabilityResult result = capabilityManager.process(
        item,
        Consumable.class,
        new QuantityCommand.Consume(0)
    );

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getPayload()).isEqualTo(0);
  }

  @Test
  @DisplayName("Consumable 수량이 부족하면 예외가 발생한다")
  void consumeCommandFailsWhenQuantityInsufficient() {
    AssetItem item = TestAssetItem.create(Consumable.multipleUse(0));

    assertThatThrownBy(() -> capabilityManager.process(
        item,
        Consumable.class,
        new QuantityCommand.Consume(1)
    )).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("Consumable + QuantityCommand.Produce 경로가 정상 동작한다")
  void processesProduceCommand() {
    AssetItem item = TestAssetItem.create(Consumable.multipleUse(2));

    CapabilityResult result = capabilityManager.process(
        item,
        Consumable.class,
        new QuantityCommand.Produce(3)
    );

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getPayload()).isEqualTo(5);
  }

  @Test
  @DisplayName("Produce 0개는 수량을 유지한다")
  void processesProduceCommandWithZeroQuantity() {
    AssetItem item = TestAssetItem.create(Consumable.multipleUse(2));

    CapabilityResult result = capabilityManager.process(
        item,
        Consumable.class,
        new QuantityCommand.Produce(0)
    );

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getPayload()).isEqualTo(2);
  }

  @Test
  @DisplayName("Acquirable + AcquirableCommand 경로가 정상 동작한다")
  void processesAcquirableCommand() {
    AssetItem item = TestAssetItem.create(Acquirable.received());

    CapabilityResult result = capabilityManager.process(
        item,
        Acquirable.class,
        new AcquirableCommand.Purchased()
    );

    assertThat(result.isSuccess()).isTrue();
    assertThat(((Acquirable) item.getCapability(Acquirable.class).orElseThrow())
        .method()).isEqualTo(Acquirable.AcquisitionMethod.PURCHASED);
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

    static TestAssetItem create(com.payvue.characteristic.AssetCharacteristic capability) {
      TestAssetItem item = new TestAssetItem(
          AssetItemId.generate(),
          CommunityId.generate(),
          UserId.generate()
      );
      item.with(capability);
      return item;
    }
  }
}
