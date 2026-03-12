package com.payvue.core.asset.characteristic;

import com.payvue.characteristic.Consumable;
import com.payvue.core.asset.characteristic.command.QuantityCommand;
import com.payvue.core.asset.AssetItem;

class ProduceHandler
    implements CommandCapabilityHandler<Consumable, QuantityCommand.Produce> {

  @Override
  public CapabilityResult apply(
      AssetItem item,
      Consumable capability,
      QuantityCommand.Produce command
  ) {
    java.util.Objects.requireNonNull(item, "item은 필수입니다.");
    java.util.Objects.requireNonNull(capability, "capability는 필수입니다.");
    java.util.Objects.requireNonNull(command, "command는 필수입니다.");
    command.apply(capability);
    return CapabilityResult.success(capability.quantity());
  }
}
