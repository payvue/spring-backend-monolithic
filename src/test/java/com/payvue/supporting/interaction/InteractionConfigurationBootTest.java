package com.payvue.supporting.interaction;

import static org.assertj.core.api.Assertions.assertThat;

import com.payvue.app.domain.community.Cooperative;
import com.payvue.app.domain.community.Role;
import com.payvue.app.domain.asset.category.Cash;
import com.payvue.app.domain.asset.category.Mileage;
import com.payvue.characteristic.HasBalance;
import com.payvue.core.asset.AssetItem;
import com.payvue.core.community.Community;
import com.payvue.core.community.Participant;
import com.payvue.core.hold.Holdings;
import com.payvue.shared.Money;
import com.payvue.supporting.user.command.UserId;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = {
    InteractionConfiguration.class
})
class InteractionConfigurationBootTest {

  @Autowired
  private InteractionExecutor interactionExecutor;

  @Test
  @DisplayName("관리 플로우 실행은 멤버를 추가하고 완료 상태로 전이된다")
  void executeManagementFlow() {
    Community community = createCooperative("org");
    Participant initiator = adminParticipant(community);
    Participant member = Participant.of(UUID.randomUUID(), Role.MEMBER);
    InviteDecision decision = new InviteDecision(new ManageIntent(initiator, member));

    Interaction interaction = interactionExecutor.executeInternal(decision, community);

    assertThat(interaction.isCompleted()).isTrue();
    assertThat(community.isParticipant(member.getUserId())).isTrue();
    assertThat(community.getParticipantRole(member.getUserId())).isEqualTo(Role.MEMBER);
  }

  @Test
  @DisplayName("결제 플로우 실행은 잔액을 차감하고 완료 상태로 전이된다")
  void executePaymentFlow() {
    Community community = createCooperative("org");
    Holdings holdings = paymentHoldings(community, 1000L);
    AssetItem item = balanceItem(community, "mileage", 300L);
    ExecutePaymentIntent intent = new ExecutePaymentIntent(
        adminParticipant(community),
        holdings,
        item
    );
    ApproveDecision<ExecutePaymentIntent> decision = new ApproveDecision<>(intent);
    AssetItem cash = holdings.getAssets().get(0);

    Interaction interaction = interactionExecutor.executeInternal(decision, community);

    HasBalance balance = cash.getCapability(HasBalance.class).orElseThrow();
    assertThat(interaction.isCompleted()).isTrue();
    assertThat(balance.balance().toLong()).isEqualTo(700L);
    assertThat(holdings.getAssets())
        .extracting(AssetItem::getName)
        .contains("mileage");
  }

  @Test
  @DisplayName("이체 플로우 실행은 자산을 이동시키고 완료 상태로 전이된다")
  void executeTransferFlow() {
    Community source = createCooperative("org");
    Community target = createCooperative("org2");
    AssetItem cash = balanceCash(source, 200L);
    Holdings from = Holdings.create(source, List.of(cash));
    Holdings to = Holdings.create(target, List.of());
    TransferAssetIntent intent = new TransferAssetIntent(
        adminParticipant(source),
        from,
        to,
        cash
    );
    ApproveDecision<TransferAssetIntent> decision = new ApproveDecision<>(intent);

    Interaction interaction = interactionExecutor.executeInternal(decision, source);

    HasBalance sourceBalance = cash.getCapability(HasBalance.class).orElseThrow();
    assertThat(interaction.isCompleted()).isTrue();
    assertThat(sourceBalance.balance().toLong()).isEqualTo(0L);
    assertThat(to.getAssets()).hasSize(1);
    HasBalance targetBalance = to.getAssets().get(0)
        .getCapability(HasBalance.class)
        .orElseThrow();
    assertThat(targetBalance.balance().toLong()).isEqualTo(200L);
  }

  private static Holdings paymentHoldings(Community community, long amount) {
    AssetItem cash = balanceCash(community, amount);
    return Holdings.create(community, List.of(cash));
  }

  private static Community createCooperative(String name) {
    return Cooperative.create(
        name,
        "desc",
        UUID.randomUUID(),
        Role.ADMIN,
        Role.MEMBER
    );
  }

  private static Participant adminParticipant(Community community) {
    return Participant.of(community.getParticipantIds().iterator().next(), Role.ADMIN);
  }

  private static AssetItem balanceCash(Community community, long amount) {
    return Cash.create(community.getId(), UserId.generate(), "cash")
        .with(HasBalance.of(Money.of(amount, "KRW")));
  }

  private static AssetItem balanceItem(Community community, String name, long amount) {
    return Mileage.create(community.getId(), UserId.generate(), name)
        .with(HasBalance.of(Money.of(amount, "KRW")));
  }
}
