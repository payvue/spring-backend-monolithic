package com.payvue.core.asset.characteristic;

import static org.assertj.core.api.Assertions.assertThat;

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

@DisplayName("ConsumableConsumePolicy")
class ConsumableConsumePolicyTest {

  @Test
  @DisplayName("수량이 충분하면 검증이 통과된다")
  void validatesWhenQuantityEnough() {
    ConsumableConsumePolicy policy = new ConsumableConsumePolicy();
    Consumable consumable = Consumable.multipleUse(5);
    AssetItem item = TestAssetItem.create(consumable);
    QuantityCommand.Consume command = new QuantityCommand.Consume(3);

    assertThat(policy.validate(item, consumable, command)).isTrue();
  }

  @Test
  @DisplayName("수량이 부족하면 검증이 실패한다")
  void failsWhenQuantityInsufficient() {
    ConsumableConsumePolicy policy = new ConsumableConsumePolicy();
    Consumable consumable = Consumable.multipleUse(2);
    AssetItem item = TestAssetItem.create(consumable);
    QuantityCommand.Consume command = new QuantityCommand.Consume(3);

    assertThat(policy.validate(item, consumable, command)).isFalse();
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
