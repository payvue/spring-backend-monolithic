package com.payvue.core.transaction.command;

import com.payvue.app.domain.asset.specification.CategorySpecification;
import com.payvue.core.asset.AssetClassification;
import com.payvue.core.asset.AssetItem;
import com.payvue.core.community.Community;
import com.payvue.core.community.Participant;
import com.payvue.core.hold.Holdings;
import com.payvue.core.hold.HoldingsRepository;
import com.payvue.supporting.interaction.ApproveDecision;
import com.payvue.supporting.interaction.Decision;
import com.payvue.supporting.interaction.InteractionExecutor;
import com.payvue.supporting.interaction.TransferAssetIntent;
import com.payvue.supporting.interaction.Interaction;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@RequiredArgsConstructor
class TransferTransactionHandler implements TransactionHandler {

  private final InteractionExecutor interactionExecutor;
  private final HoldingsRepository holdingsRepository;

  @Override
  public Transaction handle(TransactionCommand command) {
    Community community = command.actor().community();
    Holdings sender = command.actor().holdings();
    Holdings recipient = holdingsRepository.findByCommunity(command.to());

    UUID actorId = command.actor().actorId().getValue();

    AssetItem transferItem = resolveTransferItem(command, sender);

    TransferAssetIntent intent = new TransferAssetIntent(
        Participant.of(actorId, community.getParticipantRole(actorId)),
        sender,
        recipient,
        transferItem
    );
    Decision<TransferAssetIntent> decision = new ApproveDecision<>(intent);

    Interaction interaction = interactionExecutor.executeInternal(decision, community);

    if (interaction.isPendingApproval()) {
      command.item().suspend();
      return Transaction.pending(command, interaction.getStatus());
    }

    if (interaction.isCompleted()) {
      holdingsRepository.save(sender);
      holdingsRepository.save(recipient);
      return Transaction.completed(command, interaction.getStatus());
    }

    return Transaction.failed(command, interaction.getStatus());
  }

  private static AssetItem resolveTransferItem(TransactionCommand command, Holdings holdings) {
    AssetItem item = command.item();
    if (holdings == null || command.assetType() == null) {
      return item;
    }
    AssetClassification classification = resolveClassification(command.assetType());
    return holdings.ownedSingleAssetsConformingTo(new CategorySpecification(classification))
        .orElse(item);
  }

  private static AssetClassification resolveClassification(String assetType) {
    String normalized = assetType.toUpperCase(java.util.Locale.ROOT);
    if ("GIFT_CARD".equals(normalized)) {
      return AssetClassification.gift();
    }
    if ("POINT".equals(normalized)) {
      return AssetClassification.mileage();
    }
    return AssetClassification.of(normalized);
  }
}
