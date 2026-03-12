package com.payvue.supporting.interaction;

import com.payvue.app.domain.asset.category.Cash;
import com.payvue.app.domain.asset.operation.AcquireBalanceOperation;
import com.payvue.app.domain.asset.operation.DisposeBalanceOperation;
import com.payvue.app.domain.asset.specification.CategorySpecification;
import com.payvue.characteristic.HasBalance;
import com.payvue.core.asset.AssetClassification;
import com.payvue.core.asset.AssetItem;
import com.payvue.core.asset.characteristic.CapabilityManager;
import com.payvue.core.hold.Holdings;
import com.payvue.shared.Money;

final class ApproveTransferCashHandler implements InteractionHandler<TransferAssetIntent> {
  private final CapabilityManager capabilityManager;

  ApproveTransferCashHandler(CapabilityManager capabilityManager) {
    this.capabilityManager = capabilityManager;
  }

  @Override
  public boolean supports(Decision<?> decision) {
    return decision instanceof ApproveDecision;
  }

  @Override
  public void handle(InteractionContext context, Decision<? extends TransferAssetIntent> decision) {
    Holdings source = decision.getIntent().from();
    Holdings target = decision.getIntent().to();
    AssetItem transferItem = decision.getIntent().item();
    if (transferItem == null) {
      throw new IllegalArgumentException("transfer item is required");
    }
    HasBalance transferBalance = transferItem.getCapability(HasBalance.class)
        .orElseThrow(() -> new IllegalArgumentException("잔액 정보를 확인할 수 없습니다."));
    Money amount = transferBalance.balance();

    AssetItem sourceCash = source.ownedSingleAssetsConformingTo(
        new CategorySpecification(AssetClassification.cash())
    ).orElseThrow(() -> new IllegalArgumentException("캐시 자산을 찾을 수 없습니다."));

    source.dealWith(sourceCash, new DisposeBalanceOperation(amount, capabilityManager));
    AssetItem targetCash = Cash.create(target.getCommunityId(), transferItem.getContributorId(), transferItem.getName())
        .with(HasBalance.of(amount));
    target.dealWith(targetCash, new AcquireBalanceOperation(target, capabilityManager));
  }
}
