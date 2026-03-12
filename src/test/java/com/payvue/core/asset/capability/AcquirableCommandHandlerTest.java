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

@DisplayName("AcquirableCommandHandler")
class AcquirableCommandHandlerTest {

  @Test
  @DisplayName("정책 통과 시 획득 방식이 변경된다")
  void updatesAcquisitionMethod() {
    AcquirableCommandHandler handler = new AcquirableCommandHandler(new AcquirablePolicy());
    Acquirable acquirable = Acquirable.received();
    AssetItem item = TestAssetItem.create(acquirable);
    AcquirableCommand command = new AcquirableCommand.Purchased();

    CapabilityResult result = handler.apply(item, acquirable, command);

    assertThat(result.isSuccess()).isTrue();
    assertThat(acquirable.method()).isEqualTo(Acquirable.AcquisitionMethod.PURCHASED);
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
