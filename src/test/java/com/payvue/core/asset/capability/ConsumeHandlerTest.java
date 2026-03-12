package com.payvue.core.asset.characteristic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.payvue.characteristic.Consumable;
import com.payvue.core.asset.AssetClassification;
import com.payvue.core.asset.AssetItem;
import com.payvue.core.asset.AssetItemId;
import com.payvue.core.asset.AssetStatus;
import com.payvue.core.asset.characteristic.command.QuantityCommand;
import com.payvue.core.community.CommunityId;
import com.payvue.supporting.user.command.UserId;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ConsumeHandler")
class ConsumeHandlerTest {

  @Test
  @DisplayName("정책 통과 시 수량이 감소한다")
  void decreasesQuantityWhenPolicyPasses() {
    ConsumeHandler handler = new ConsumeHandler(new ConsumableConsumePolicy());
    Consumable consumable = Consumable.multipleUse(5);
    AssetItem item = TestAssetItem.create(consumable);
    QuantityCommand.Consume command = new QuantityCommand.Consume(3);

    CapabilityResult result = handler.apply(item, consumable, command);

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getPayload()).isEqualTo(2);
  }

  @Test
  @DisplayName("수량이 정확히 같으면 차감이 허용된다")
  void allowsWhenQuantityEquals() {
    ConsumeHandler handler = new ConsumeHandler(new ConsumableConsumePolicy());
    Consumable consumable = Consumable.multipleUse(3);
    AssetItem item = TestAssetItem.create(consumable);
    QuantityCommand.Consume command = new QuantityCommand.Consume(3);

    CapabilityResult result = handler.apply(item, consumable, command);

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getPayload()).isEqualTo(0);
  }

  @Test
  @DisplayName("수량이 부족하면 예외가 발생한다")
  void failsWhenQuantityInsufficient() {
    ConsumeHandler handler = new ConsumeHandler(new ConsumableConsumePolicy());
    Consumable consumable = Consumable.multipleUse(2);
    AssetItem item = TestAssetItem.create(consumable);
    QuantityCommand.Consume command = new QuantityCommand.Consume(3);

    assertThatThrownBy(() -> handler.apply(item, consumable, command))
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

    static TestAssetItem create(Consumable consumable) {
      TestAssetItem item = new TestAssetItem(
          AssetItemId.generate(),
          CommunityId.generate(),
          UserId.generate()
      );
      item.with(consumable);
      return item;
    }
  }
}
