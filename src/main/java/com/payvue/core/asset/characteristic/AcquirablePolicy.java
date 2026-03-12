package com.payvue.core.asset.characteristic;

import com.payvue.characteristic.Acquirable;
import com.payvue.core.asset.characteristic.command.AcquirableCommand;
import com.payvue.core.asset.AssetItem;

class AcquirablePolicy implements CapabilityPolicy<AcquirableCommand, Acquirable> {
  @Override
  public boolean validate(AssetItem item, Acquirable capability, AcquirableCommand command) {
    return item != null && capability != null && command != null;
  }
}
