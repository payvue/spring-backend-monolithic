package com.payvue.supporting.interaction;

import static org.assertj.core.api.Assertions.assertThat;

import com.payvue.app.domain.asset.category.Cash;
import com.payvue.app.domain.asset.category.Coupon;
import com.payvue.app.domain.community.Cooperative;
import com.payvue.app.domain.community.Role;
import com.payvue.characteristic.Consumable;
import com.payvue.characteristic.HasBalance;
import com.payvue.core.asset.AssetItem;
import com.payvue.core.community.Community;
import com.payvue.core.hold.Holdings;
import com.payvue.shared.Money;
import com.payvue.supporting.user.command.UserId;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ApproveTransferAssetHandlerTest {

  @Test
  @DisplayName("이체 승인은 소스 잔액을 차감하고 타깃에 자산을 생성한다")
  void approveTransferMovesBalance() {
    Community sourceCommunity = Cooperative.create(
        "org",
        "desc",
        UUID.randomUUID(),
        Role.ADMIN,
        Role.MEMBER
    );
    Community targetCommunity = Cooperative.create(
        "org2",
        "desc2",
        UUID.randomUUID(),
        Role.ADMIN,
        Role.MEMBER
    );
    AssetItem cash = Cash.create(sourceCommunity.getId(), UserId.generate(), "cash")
        .with(HasBalance.of(Money.of(200L, "KRW")));
    Holdings source = Holdings.create(sourceCommunity, List.of(cash));
    Holdings target = Holdings.create(targetCommunity, List.of());

    TransferAssetIntent intent = new TransferAssetIntent(
        com.payvue.core.community.Participant.of(sourceCommunity.getParticipantIds().iterator().next(), Role.ADMIN),
        source,
        target,
        cash
    );
    ApproveDecision<TransferAssetIntent> decision = new ApproveDecision<>(intent);

    ApproveTransferAssetHandler handler = new ApproveTransferAssetHandler();
    handler.handle(new InternalInteractionContext(sourceCommunity), decision);

    assertThat(source.getAssets()).isEmpty();
    assertThat(target.getAssets()).hasSize(1);
    HasBalance targetBalance = target.getAssets().get(0)
        .getCapability(HasBalance.class)
        .orElseThrow();
    assertThat(targetBalance.balance().toLong()).isEqualTo(200L);
  }

  @Test
  @DisplayName("소모성 자산 이체는 소스에서 제거되고 타깃에 추가된다")
  void approveTransferMovesConsumable() {
    Community sourceCommunity = Cooperative.create(
        "org",
        "desc",
        UUID.randomUUID(),
        Role.ADMIN,
        Role.MEMBER
    );
    Community targetCommunity = Cooperative.create(
        "org2",
        "desc2",
        UUID.randomUUID(),
        Role.ADMIN,
        Role.MEMBER
    );
    AssetItem coupon = Coupon.create(sourceCommunity.getId(), UserId.generate(), "coupon")
        .with(Consumable.singleUse(1));
    Holdings source = Holdings.create(sourceCommunity, List.of(coupon));
    Holdings target = Holdings.create(targetCommunity, List.of());

    TransferAssetIntent intent = new TransferAssetIntent(
        com.payvue.core.community.Participant.of(sourceCommunity.getParticipantIds().iterator().next(), Role.ADMIN),
        source,
        target,
        coupon
    );
    ApproveDecision<TransferAssetIntent> decision = new ApproveDecision<>(intent);

    ApproveTransferAssetHandler handler = new ApproveTransferAssetHandler();
    handler.handle(new InternalInteractionContext(sourceCommunity), decision);

    assertThat(source.getAssets()).isEmpty();
    assertThat(target.getAssets())
        .extracting(AssetItem::getName)
        .contains("coupon");
    assertThat(target.getAssets())
            .extracting(item -> item.getCapability(Consumable.class).get().quantity())
            .containsExactly(1);
  }
}
