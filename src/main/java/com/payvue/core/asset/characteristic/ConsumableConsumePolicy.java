package com.payvue.core.asset.characteristic;

import com.payvue.characteristic.Consumable;
import com.payvue.core.asset.characteristic.command.QuantityCommand;
import com.payvue.core.asset.AssetItem;

public class ConsumableConsumePolicy
    implements CapabilityPolicy<QuantityCommand.Consume, Consumable> {

  @Override
  public boolean validate(
      AssetItem item,
      Consumable capability,
      QuantityCommand.Consume command
  ) {
    if (item == null || capability == null || command == null) {
      return false;
    }
    return capability.quantity() >= command.getQuantity();
  }
}
