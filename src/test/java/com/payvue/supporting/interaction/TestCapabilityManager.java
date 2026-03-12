package com.payvue.supporting.interaction;

import com.payvue.characteristic.Consumable;
import com.payvue.characteristic.HasBalance;
import com.payvue.core.asset.AssetItem;
import com.payvue.core.asset.characteristic.CapabilityCommand;
import com.payvue.core.asset.characteristic.CapabilityManager;
import com.payvue.core.asset.characteristic.CapabilityResult;
import com.payvue.core.asset.characteristic.command.DepositCommand;
import com.payvue.core.asset.characteristic.command.PayableCommand;
import com.payvue.core.asset.characteristic.command.QuantityCommand;

final class TestCapabilityManager implements CapabilityManager {

  @Override
  public <T extends com.payvue.characteristic.AssetCharacteristic> CapabilityResult process(
      AssetItem item,
      Class<T> capabilityType,
      CapabilityCommand command
  ) {
    if (item == null || capabilityType == null || command == null) {
      return CapabilityResult.failure("invalid");
    }

    if (capabilityType.equals(HasBalance.class)) {
      return applyBalanceCommand(item, command);
    }
    if (capabilityType.equals(Consumable.class)) {
      return applyConsumableCommand(item, command);
    }
    return CapabilityResult.failure("unsupported");
  }

  private CapabilityResult applyBalanceCommand(AssetItem item, CapabilityCommand command) {
    HasBalance balance = item.getCapability(HasBalance.class)
        .orElseThrow(() -> new IllegalStateException("HasBalance 능력 없음"));
    if (command instanceof DepositCommand deposit) {
      deposit.apply(balance);
      return CapabilityResult.success(balance.balance());
    }
    if (command instanceof PayableCommand pay) {
      pay.apply(balance);
      return CapabilityResult.success(balance.balance());
    }
    return CapabilityResult.failure("unsupported");
  }

  private CapabilityResult applyConsumableCommand(AssetItem item, CapabilityCommand command) {
    Consumable consumable = item.getCapability(Consumable.class)
        .orElseThrow(() -> new IllegalStateException("Consumable 능력 없음"));
    if (command instanceof QuantityCommand.Consume consume) {
      consume.apply(consumable);
      return CapabilityResult.success(consumable.quantity());
    }
    if (command instanceof QuantityCommand.Produce produce) {
      produce.apply(consumable);
      return CapabilityResult.success(consumable.quantity());
    }
    return CapabilityResult.failure("unsupported");
  }
}
