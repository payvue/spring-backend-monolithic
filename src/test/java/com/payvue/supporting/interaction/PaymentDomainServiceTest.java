package com.payvue.supporting.interaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.payvue.app.domain.asset.category.Cash;
import com.payvue.app.domain.asset.category.Mileage;
import com.payvue.app.domain.community.Cooperative;
import com.payvue.app.domain.community.Role;
import com.payvue.characteristic.HasBalance;
import com.payvue.core.asset.AssetItem;
import com.payvue.core.community.Community;
import com.payvue.core.hold.Holdings;
import com.payvue.shared.Money;
import com.payvue.supporting.user.command.UserId;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PaymentDomainServiceTest {

  @Test
  @DisplayName("PAYMENT 카테고리 결정은 지원한다")
  void supportsPaymentCategory() {
    PaymentDomainService service = (PaymentDomainService) PaymentDomainService.of();
    ExecutePaymentIntent intent = new ExecutePaymentIntent(
        ParticipantFixtures.admin(Role.ADMIN),
        HoldingsFixtures.emptyHoldings(),
        null
    );
    ApproveDecision<ExecutePaymentIntent> decision = new ApproveDecision<>(intent);

    assertThat(service.supports(decision)).isTrue();
  }

  @Test
  @DisplayName("잔액이 충분하면 핸들러를 실행하고 승인 대기 상태를 반환한다")
  void executeInvokesHandlerWhenBalanceIsEnough() {
    Community community = Cooperative.create(
        "org",
        "desc",
        UUID.randomUUID(),
        Role.ADMIN,
        Role.MEMBER
    );
    Holdings holdings = HoldingsFixtures.cashHoldings(community, 1000);
    AssetItem payable = Mileage.create(community.getId(), UserId.generate(), "payable")
        .with(HasBalance.of(Money.of(300L, "KRW")));
    ExecutePaymentIntent intent = new ExecutePaymentIntent(
        ParticipantFixtures.admin(Role.ADMIN, community.getParticipantIds().iterator().next()),
        holdings,
        payable
    );
    ApproveDecision<ExecutePaymentIntent> decision = new ApproveDecision<>(intent);
    InternalInteractionContext context = new InternalInteractionContext(community);
    AtomicBoolean called = new AtomicBoolean(false);
    InteractionHandler<ExecutePaymentIntent> handler = new RecordingHandler(called);

    Interaction interaction = PaymentDomainService.of().execute(context, decision, handler);

    assertThat(called.get()).isTrue();
    assertThat(interaction.isPendingApproval()).isTrue();
  }

  @Test
  @DisplayName("잔액이 부족하면 예외가 발생한다")
  void executeThrowsWhenBalanceIsInsufficient() {
    Community community = Cooperative.create(
        "org",
        "desc",
        UUID.randomUUID(),
        Role.ADMIN,
        Role.MEMBER
    );
    Holdings holdings = HoldingsFixtures.cashHoldings(community, 100);
    AssetItem payable = Mileage.create(community.getId(), UserId.generate(), "payable")
        .with(HasBalance.of(Money.of(300L, "KRW")));
    ExecutePaymentIntent intent = new ExecutePaymentIntent(
        ParticipantFixtures.admin(Role.ADMIN, community.getParticipantIds().iterator().next()),
        holdings,
        payable
    );
    ApproveDecision<ExecutePaymentIntent> decision = new ApproveDecision<>(intent);
    InternalInteractionContext context = new InternalInteractionContext(community);

    assertThatThrownBy(() -> PaymentDomainService.of().execute(context, decision, new RecordingHandler(new AtomicBoolean())))
        .isInstanceOf(IllegalArgumentException.class);
  }

  static final class RecordingHandler implements InteractionHandler<ExecutePaymentIntent> {
    private final AtomicBoolean called;

    RecordingHandler(AtomicBoolean called) {
      this.called = called;
    }

    @Override
    public boolean supports(Decision<?> decision) {
      return decision instanceof ApproveDecision;
    }

    @Override
    public void handle(InteractionContext context, Decision<? extends ExecutePaymentIntent> decision) {
      called.set(true);
    }
  }

  static final class HoldingsFixtures {
    static Holdings cashHoldings(Community community, long amount) {
      AssetItem cash = Cash.create(community.getId(), UserId.generate(), "cash")
          .with(HasBalance.of(Money.of(amount, "KRW")));
      return Holdings.create(community, List.of(cash));
    }

    static Holdings emptyHoldings() {
      Community community = Cooperative.create(
          "org",
          "desc",
          UUID.randomUUID(),
          Role.ADMIN,
          Role.MEMBER
      );
      return Holdings.create(community, List.of());
    }
  }

  static final class ParticipantFixtures {
    static com.payvue.core.community.Participant admin(Role role, UUID id) {
      return com.payvue.core.community.Participant.of(id, role);
    }

    static com.payvue.core.community.Participant admin(Role role) {
      return com.payvue.core.community.Participant.of(UUID.randomUUID(), role);
    }
  }
}
