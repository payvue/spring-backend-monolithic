package com.payvue.core.asset.characteristic;

import com.payvue.characteristic.HasBalance;
import com.payvue.core.asset.characteristic.command.DepositCommand;
import com.payvue.core.asset.AssetItem;

class BalanceDepositHandler implements CommandCapabilityHandler<HasBalance, DepositCommand> {
  private final CapabilityPolicy<DepositCommand, HasBalance> policy;

  public BalanceDepositHandler(CapabilityPolicy<DepositCommand, HasBalance> policy) {
    this.policy = java.util.Objects.requireNonNull(policy, "policy는 필수입니다.");
  }

  @Override
  public CapabilityResult apply(AssetItem item, HasBalance capability, DepositCommand command) {
    java.util.Objects.requireNonNull(item, "item은 필수입니다.");
    java.util.Objects.requireNonNull(capability, "capability는 필수입니다.");
    java.util.Objects.requireNonNull(command, "command는 필수입니다.");
    policy.validateOrThrow(item, capability, command);
    command.apply(capability);
    return CapabilityResult.success(capability.balance());
  }
}
