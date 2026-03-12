package com.payvue.core.asset.characteristic;

import com.payvue.characteristic.Consumable;
import com.payvue.core.asset.characteristic.command.QuantityCommand;
import com.payvue.core.asset.AssetItem;

class ConsumeHandler
    implements CommandCapabilityHandler<Consumable, QuantityCommand.Consume> {

  private final CapabilityPolicy<QuantityCommand.Consume, Consumable> policy;

  public ConsumeHandler(
      CapabilityPolicy<QuantityCommand.Consume, Consumable> policy
  ) {
    this.policy = java.util.Objects.requireNonNull(policy, "policy는 필수입니다.");
  }

  @Override
  public CapabilityResult apply(
      AssetItem item,
      Consumable capability,
      QuantityCommand.Consume command
  ) {
    java.util.Objects.requireNonNull(item, "item은 필수입니다.");
    java.util.Objects.requireNonNull(capability, "capability는 필수입니다.");
    java.util.Objects.requireNonNull(command, "command는 필수입니다.");
    policy.validateOrThrow(item, capability, command);
    command.apply(capability);
    return CapabilityResult.success(capability.quantity());
  }
}
