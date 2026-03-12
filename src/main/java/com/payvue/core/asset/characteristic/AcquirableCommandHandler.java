package com.payvue.core.asset.characteristic;

import com.payvue.characteristic.Acquirable;
import com.payvue.core.asset.characteristic.command.AcquirableCommand;
import com.payvue.core.asset.AssetItem;

class AcquirableCommandHandler
    implements CommandCapabilityHandler<Acquirable, AcquirableCommand> {
  private final CapabilityPolicy<AcquirableCommand, Acquirable> policy;

  public AcquirableCommandHandler(CapabilityPolicy<AcquirableCommand, Acquirable> policy) {
    this.policy = java.util.Objects.requireNonNull(policy, "policy는 필수입니다.");
  }

  @Override
  public CapabilityResult apply(
      AssetItem item,
      Acquirable capability,
      AcquirableCommand command
  ) {
    java.util.Objects.requireNonNull(item, "item은 필수입니다.");
    java.util.Objects.requireNonNull(capability, "capability는 필수입니다.");
    java.util.Objects.requireNonNull(command, "command는 필수입니다.");
    policy.validateOrThrow(item, capability, command);
    command.apply(capability);
    return CapabilityResult.success(capability.method());
  }
}
