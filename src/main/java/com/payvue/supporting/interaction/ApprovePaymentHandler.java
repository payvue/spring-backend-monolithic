package com.payvue.supporting.interaction;

import com.payvue.characteristic.HasBalance;
import com.payvue.characteristic.Consumable;
import com.payvue.core.asset.AssetClassification;
import com.payvue.core.asset.AssetItem;
import com.payvue.app.domain.asset.category.Cash;
import com.payvue.core.asset.characteristic.CapabilityManager;
import com.payvue.core.hold.AssetOperationUnit;
import com.payvue.core.hold.Holdings;
import com.payvue.app.domain.asset.operation.AcquireBalanceOperation;
import com.payvue.app.domain.asset.operation.AcquireConsumableOperation;
import com.payvue.app.domain.asset.specification.CategorySpecification;

final class ApprovePaymentHandler implements InteractionHandler<ExecutePaymentIntent> {
  private final CapabilityManager capabilityManager;

  ApprovePaymentHandler(CapabilityManager capabilityManager) {
    this.capabilityManager = capabilityManager;
  }

  @Override
  public boolean supports(Decision<?> decision) {
    return decision instanceof ApproveDecision;
  }

  @Override
  public void handle(InteractionContext context, Decision<? extends ExecutePaymentIntent> decision) {
    Holdings hold = decision.getIntent().holdings();
    AssetItem item = decision.getIntent().item();

    Cash cash = hold.ownedSingleAssetsConformingTo(new CategorySpecification(AssetClassification.cash()))
        .filter(Cash.class::isInstance)
        .map(Cash.class::cast)
        .get();

    HasBalance credit = cash.getCapability(HasBalance.class)
        .orElseThrow(IllegalArgumentException::new);
    item.getCapability(HasBalance.class)
        .ifPresent(payable -> credit.deduct(payable.balance()));
    if (!item.hasCapability(HasBalance.class)) {
      cash.getCapability(Consumable.class)
          .ifPresentOrElse(existing -> existing.setQuantity(0), () -> cash.with(Consumable.singleUse(0)));
    }

    AssetOperationUnit unit = item.hasCapability(HasBalance.class)
        ? new AcquireBalanceOperation(hold, capabilityManager)
        : new AcquireConsumableOperation(hold, capabilityManager);

    hold.dealWith(item, unit);

  }
}
