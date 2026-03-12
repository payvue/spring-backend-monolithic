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

@DisplayName("ProduceHandler")
class ProduceHandlerTest {

  @Test
  @DisplayName("정상 입력이면 수량이 증가한다")
  void increasesQuantity() {
    ProduceHandler handler = new ProduceHandler();
    Consumable consumable = Consumable.multipleUse(2);
    AssetItem item = TestAssetItem.create(consumable);
    QuantityCommand.Produce command = new QuantityCommand.Produce(3);

    CapabilityResult result = handler.apply(item, consumable, command);

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getPayload()).isEqualTo(5);
  }

  @Test
  @DisplayName("필수 입력이 누락되면 예외가 발생한다")
  void failsWhenArgumentsMissing() {
    ProduceHandler handler = new ProduceHandler();
    Consumable consumable = Consumable.multipleUse(2);
    AssetItem item = TestAssetItem.create(consumable);

    assertThatThrownBy(() -> handler.apply(item, consumable, null))
        .isInstanceOf(NullPointerException.class);
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
