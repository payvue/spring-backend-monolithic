package com.payvue.core.asset.characteristic;

import com.payvue.characteristic.HasBalance;
import com.payvue.core.asset.characteristic.command.PayableCommand;
import com.payvue.core.asset.AssetItem;

class PayablePolicy implements CapabilityPolicy<PayableCommand, HasBalance> {
  @Override
  public boolean validate(AssetItem item, HasBalance capability, PayableCommand command) {
    if (item == null || capability == null || command == null) {
      return false;
    }
    if (command.amount() == null || command.amount().isNegative()) {
      return false;
    }
    return !capability.balance().isLessThan(command.amount());
  }
}
