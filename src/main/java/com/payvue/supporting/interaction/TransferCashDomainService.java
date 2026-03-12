package com.payvue.supporting.interaction;

import com.payvue.characteristic.HasBalance;
import com.payvue.core.asset.AssetItem;

final class TransferCashDomainService implements DomainService<TransferAssetIntent> {

  private static DomainService instance = new TransferCashDomainService();

  public static DomainService of() {
    if (instance == null) {
      instance = new TransferCashDomainService();
    }
    return instance;
  }

  @Override
  public boolean supports(Decision<?> decision) {
    if (decision == null || decision.getIntent() == null) {
      return false;
    }
    if (!IntentCategory.TRANSFER.name().equals(decision.getIntent().getCategory())) {
      return false;
    }
    if (!(decision.getIntent() instanceof TransferAssetIntent transferIntent)) {
      return false;
    }
    AssetItem item = transferIntent.item();
    return item != null
        && item.getCategory() != null
        && item.getCategory().isCash()
        && item.hasCapability(HasBalance.class);
  }

  @Override
  public Interaction execute(Decision<TransferAssetIntent> decision) {
    throw new UnsupportedOperationException("컨텍스트 기반 실행이 필요합니다.");
  }

  @Override
  public Interaction execute(
      InteractionContext context,
      Decision<TransferAssetIntent> decision,
      InteractionHandler<TransferAssetIntent> handler
  ) {
    Interaction interaction = Interaction.pending(decision);
    handler.handle(context, decision);
    if (handler instanceof ApproveTransferCashHandler) {
      interaction.complete();
    }
    return interaction;
  }
}
