package com.payvue.supporting.interaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.payvue.app.domain.asset.category.Cash;
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

class TransferDomainServiceTest {

  @Test
  @DisplayName("이체 자산이 보유 목록에 있으면 핸들러를 실행한다")
  void executeInvokesHandlerWhenAssetExists() {
    Community community = Cooperative.create(
        "org",
        "desc",
        UUID.randomUUID(),
        Role.ADMIN,
        Role.MEMBER
    );
    AssetItem cash = Cash.create(community.getId(), UserId.generate(), "cash")
        .with(HasBalance.of(Money.of(300L, "KRW")));
    Holdings from = Holdings.create(community, List.of(cash));
    Holdings to = Holdings.create(community, List.of());
    TransferAssetIntent intent = new TransferAssetIntent(
        com.payvue.core.community.Participant.of(community.getParticipantIds().iterator().next(), Role.ADMIN),
        from,
        to,
        cash
    );
    ApproveDecision<TransferAssetIntent> decision = new ApproveDecision<>(intent);
    InternalInteractionContext context = new InternalInteractionContext(community);
    AtomicBoolean called = new AtomicBoolean(false);
    InteractionHandler<TransferAssetIntent> handler = new RecordingHandler(called);

    Interaction interaction = TransferDomainService.of().execute(context, decision, handler);

    assertThat(called.get()).isTrue();
    assertThat(interaction.isPendingApproval()).isTrue();
  }

  @Test
  @DisplayName("보유하지 않은 자산이면 예외가 발생한다")
  void executeThrowsWhenAssetMissing() {
    Community community = Cooperative.create(
        "org",
        "desc",
        UUID.randomUUID(),
        Role.ADMIN,
        Role.MEMBER
    );
    Holdings from = Holdings.create(community, List.of());
    Holdings to = Holdings.create(community, List.of());
    AssetItem cash = Cash.create(community.getId(), UserId.generate(), "cash")
        .with(HasBalance.of(Money.of(300L, "KRW")));
    TransferAssetIntent intent = new TransferAssetIntent(
        com.payvue.core.community.Participant.of(community.getParticipantIds().iterator().next(), Role.ADMIN),
        from,
        to,
        cash
    );
    ApproveDecision<TransferAssetIntent> decision = new ApproveDecision<>(intent);
    InternalInteractionContext context = new InternalInteractionContext(community);

    assertThatThrownBy(() -> TransferDomainService.of().execute(context, decision, new RecordingHandler(new AtomicBoolean())))
        .isInstanceOf(IllegalArgumentException.class);
  }

  static final class RecordingHandler implements InteractionHandler<TransferAssetIntent> {
    private final AtomicBoolean called;

    RecordingHandler(AtomicBoolean called) {
      this.called = called;
    }

    @Override
    public boolean supports(Decision<?> decision) {
      return decision instanceof ApproveDecision;
    }

    @Override
    public void handle(InteractionContext context, Decision<? extends TransferAssetIntent> decision) {
      called.set(true);
    }
  }
}
