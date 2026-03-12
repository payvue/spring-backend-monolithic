package com.payvue.core.asset.characteristic;

import com.payvue.characteristic.HasBalance;
import com.payvue.core.asset.characteristic.command.DepositCommand;
import com.payvue.core.asset.AssetItem;

class DepositPolicy implements CapabilityPolicy<DepositCommand, HasBalance> {
  @Override
  public boolean validate(AssetItem item, HasBalance capability, DepositCommand command) {
    if (item == null || capability == null || command == null) {
      return false;
    }
    return command.amount() != null && !command.amount().isNegative();
  }
}
