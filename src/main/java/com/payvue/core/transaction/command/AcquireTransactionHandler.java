package com.payvue.core.transaction.command;

import com.payvue.app.domain.asset.category.Cash;
import com.payvue.app.domain.asset.operation.AcquireBalanceOperation;
import com.payvue.app.domain.asset.operation.AcquireConsumableOperation;
import com.payvue.characteristic.HasBalance;
import com.payvue.common.status.ExecutionState;
import com.payvue.core.asset.AssetItem;
import com.payvue.core.asset.characteristic.CapabilityManager;
import com.payvue.core.hold.Holdings;
import com.payvue.core.hold.HoldingsRepository;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
final class AcquireTransactionHandler implements TransactionHandler {

  private final CapabilityManager capabilityManager;
  private final HoldingsRepository holdingsRepository;

  @Override
  public Transaction handle(TransactionCommand command) {
    Holdings holdings = command.actor().holdings();
    if (holdings == null) {
      return Transaction.failed(command, ExecutionState.FAILED);
    }
    try {

      AssetItem targetItem = command.item();
      if ("CASH".equalsIgnoreCase(command.assetType())) {
        targetItem = Cash.create(
            command.actor().community().getId(),
            command.actor().actorId(),
            "CASH"
        ).with(HasBalance.of(command.amount()));
      }

      if (targetItem.hasCapability(HasBalance.class)) {
        holdings.dealWith(targetItem, new AcquireBalanceOperation(holdings, capabilityManager));
      } else {
        holdings.dealWith(targetItem, new AcquireConsumableOperation(holdings, capabilityManager));
      }

      holdingsRepository.save(holdings);
      return Transaction.completed(command, ExecutionState.COMPLETED);
    } catch (RuntimeException ex) {
      return Transaction.failed(command, ExecutionState.FAILED);
    }
  }
}
