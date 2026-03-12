package com.payvue.supporting.interaction;

import static org.assertj.core.api.Assertions.assertThat;

import com.payvue.app.domain.asset.category.Cash;
import com.payvue.app.domain.asset.category.Mileage;
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

class ApprovePaymentHandlerTest {

  @Test
  @DisplayName("결제 승인은 캐시 잔액을 차감하고 자산을 보유 목록에 추가한다")
  void approvePaymentDeductsCashAndAddsAsset() {
    Community community = Cooperative.create(
        "org",
        "desc",
        UUID.randomUUID(),
        Role.ADMIN,
        Role.MEMBER
    );
    AssetItem cash = Cash.create(community.getId(), UserId.generate(), "cash")
        .with(HasBalance.of(Money.of(1000L, "KRW")));
    Holdings holdings = Holdings.create(community, List.of(cash));
    AssetItem mileage = Mileage.create(community.getId(), UserId.generate(), "mileage")
        .with(HasBalance.of(Money.of(300L, "KRW")));

    ExecutePaymentIntent intent = new ExecutePaymentIntent(
        com.payvue.core.community.Participant.of(community.getParticipantIds().iterator().next(), Role.ADMIN),
        holdings,
        mileage
    );
    ApproveDecision<ExecutePaymentIntent> decision = new ApproveDecision<>(intent);
    ApprovePaymentHandler handler = new ApprovePaymentHandler(new TestCapabilityManager());

    handler.handle(new InternalInteractionContext(community), decision);

    HasBalance cashBalance = cash.getCapability(HasBalance.class).orElseThrow();
    assertThat(cashBalance.balance().toLong()).isEqualTo(700L);
    assertThat(holdings.getAssets())
        .extracting(AssetItem::getName)
        .contains("mileage");
  }

  @Test
  @DisplayName("소모성 자산 결제는 잔액 차감 없이 보유 목록에 추가된다")
  void approvePaymentAddsConsumableWithoutDeductingCash() {
    Community community = Cooperative.create(
        "org",
        "desc",
        UUID.randomUUID(),
        Role.ADMIN,
        Role.MEMBER
    );
    AssetItem cash = Cash.create(community.getId(), UserId.generate(), "cash")
        .with(HasBalance.of(Money.of(1000L, "KRW")));
    Holdings holdings = Holdings.create(community, List.of(cash));
    AssetItem coupon = Coupon.create(community.getId(), UserId.generate(), "coupon")
        .with(Consumable.singleUse(1));

    ExecutePaymentIntent intent = new ExecutePaymentIntent(
        com.payvue.core.community.Participant.of(community.getParticipantIds().iterator().next(), Role.ADMIN),
        holdings,
        coupon
    );
    ApproveDecision<ExecutePaymentIntent> decision = new ApproveDecision<>(intent);
    ApprovePaymentHandler handler = new ApprovePaymentHandler(new TestCapabilityManager());

    handler.handle(new InternalInteractionContext(community), decision);

    Consumable consumable = cash.getCapability(Consumable.class).orElseThrow();
    assertThat(consumable.quantity()).isEqualTo(0);
    assertThat(holdings.getAssets())
        .extracting(AssetItem::getName)
        .contains("coupon");
  }
}
