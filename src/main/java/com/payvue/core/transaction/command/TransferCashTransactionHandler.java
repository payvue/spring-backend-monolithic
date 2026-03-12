package com.payvue.core.transaction.command;

import com.payvue.app.domain.asset.category.Cash;
import com.payvue.app.domain.asset.specification.CategorySpecification;
import com.payvue.characteristic.HasBalance;
import com.payvue.core.asset.AssetClassification;
import com.payvue.core.asset.AssetItem;
import com.payvue.core.community.Community;
import com.payvue.core.community.Participant;
import com.payvue.core.hold.Holdings;
import com.payvue.shared.Money;
import com.payvue.supporting.interaction.ApproveDecision;
import com.payvue.supporting.interaction.Decision;
import com.payvue.supporting.interaction.Interaction;
import com.payvue.supporting.interaction.InteractionExecutor;
import com.payvue.supporting.interaction.TransferAssetIntent;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
final class TransferCashTransactionHandler implements TransactionHandler {

  private final InteractionExecutor interactionExecutor;

  @Override
  public Transaction handle(TransactionCommand command) {
    Community community = command.actor().community();
    Holdings holdings = command.actor().holdings();
    UUID actorId = command.actor().actorId().getValue();

    Money amount = command.amount();
    if (amount == null || amount.isNegativeOrZero()) {
      throw new IllegalArgumentException("송금 금액이 유효하지 않습니다.");
    }

    AssetItem cashItem = resolveCashItem(command, holdings);
    HasBalance balance = cashItem.getCapability(HasBalance.class)
        .orElseThrow(() -> new IllegalArgumentException("잔액 정보를 확인할 수 없습니다."));
    if (balance.balance().isLessThan(amount)) {
      throw new IllegalArgumentException("잔액이 부족합니다.");
    }

    AssetItem transferItem = Cash.create(
        community.getId(),
        command.actor().actorId(),
        cashItem.getName()
    ).with(HasBalance.of(amount));

    TransferAssetIntent intent = new TransferAssetIntent(
        Participant.of(actorId, community.getParticipantRole(actorId)),
        holdings,
        holdings,
        transferItem
    );
    Decision<TransferAssetIntent> decision = new ApproveDecision<>(intent);

    Interaction interaction = interactionExecutor.executeInternal(decision, community);

    if (interaction.isPendingApproval()) {
      command.item().suspend();
      return Transaction.pending(command, interaction.getStatus());
    }

    if (interaction.isCompleted()) {
      return Transaction.completed(command, interaction.getStatus());
    }

    return Transaction.failed(command, interaction.getStatus());
  }

  private static AssetItem resolveCashItem(TransactionCommand command, Holdings holdings) {
    AssetItem item = command.item();
    if (holdings == null) {
      return item;
    }
    return holdings.ownedSingleAssetsConformingTo(
        new CategorySpecification(AssetClassification.cash())
    ).orElse(item);
  }
}
