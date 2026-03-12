package com.payvue.app.domain.asset.operation;

import static org.assertj.core.api.Assertions.assertThat;

import com.payvue.core.asset.AssetClassification;
import com.payvue.core.asset.AssetItem;
import com.payvue.core.asset.AssetItemId;
import com.payvue.core.asset.AssetStatus;
import com.payvue.core.community.CommunityId;
import com.payvue.core.hold.OperationResult;
import com.payvue.supporting.user.command.UserId;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ExpireOperation")
class ExpireOperationTest {

  @Test
  @DisplayName("활성 자산은 만료 처리된다")
  void expiresActiveAsset() {
    AssetItem asset = TestAssetItem.create(AssetStatus.ACTIVE);
    ExpireOperation operation = new ExpireOperation("reason");

    OperationResult result = operation.execute(asset);

    assertThat(result.isSuccess()).isTrue();
    assertThat(asset.getStatus()).isEqualTo(AssetStatus.EXPIRED);
  }

  @Test
  @DisplayName("이미 비활성인 자산은 실패 처리된다")
  void failsWhenAssetInactive() {
    AssetItem asset = TestAssetItem.create(AssetStatus.EXPIRED);
    ExpireOperation operation = new ExpireOperation("reason");

    OperationResult result = operation.execute(asset);

    assertThat(result.isFailure()).isTrue();
  }

  static final class TestAssetItem extends AssetItem {
    private TestAssetItem(
        AssetItemId id,
        CommunityId communityId,
        UserId contributorId,
        AssetStatus status
    ) {
      super(
          id,
          "asset",
          communityId,
          contributorId,
          AssetClassification.cash(),
          Set.of(),
          status
      );
    }

    static TestAssetItem create(AssetStatus status) {
      return new TestAssetItem(
          AssetItemId.generate(),
          CommunityId.generate(),
          UserId.generate(),
          status
      );
    }
  }
}
