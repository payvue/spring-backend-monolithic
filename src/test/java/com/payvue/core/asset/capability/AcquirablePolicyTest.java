package com.payvue.core.asset.characteristic;

import static org.assertj.core.api.Assertions.assertThat;

import com.payvue.characteristic.Acquirable;
import com.payvue.core.asset.AssetClassification;
import com.payvue.core.asset.AssetItem;
import com.payvue.core.asset.AssetItemId;
import com.payvue.core.asset.AssetStatus;
import com.payvue.core.asset.characteristic.command.AcquirableCommand;
import com.payvue.core.community.CommunityId;
import com.payvue.supporting.user.command.UserId;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AcquirablePolicy")
class AcquirablePolicyTest {

  @Test
  @DisplayName("필수 값이 존재하면 검증이 통과된다")
  void validatesWhenValuesPresent() {
    AcquirablePolicy policy = new AcquirablePolicy();
    Acquirable acquirable = Acquirable.purchased();
    AssetItem item = TestAssetItem.create(acquirable);
    AcquirableCommand command = new AcquirableCommand.Purchased();

    assertThat(policy.validate(item, acquirable, command)).isTrue();
  }

  @Test
  @DisplayName("값이 누락되면 검증이 실패한다")
  void failsWhenValueMissing() {
    AcquirablePolicy policy = new AcquirablePolicy();
    Acquirable acquirable = Acquirable.purchased();
    AcquirableCommand command = new AcquirableCommand.Purchased();

    assertThat(policy.validate(null, acquirable, command)).isFalse();
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

    static TestAssetItem create(Acquirable acquirable) {
      TestAssetItem item = new TestAssetItem(
          AssetItemId.generate(),
          CommunityId.generate(),
          UserId.generate()
      );
      item.with(acquirable);
      return item;
    }
  }
}
