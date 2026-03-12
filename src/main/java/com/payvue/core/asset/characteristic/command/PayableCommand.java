package com.payvue.core.asset.characteristic.command;

import com.payvue.characteristic.HasBalance;
import com.payvue.core.asset.characteristic.CapabilityCommand;
import com.payvue.shared.Money;

public interface PayableCommand extends CapabilityCommand {
  Money amount();

  default void apply(HasBalance balance) {
    balance.deduct(amount());
  }

  final class Pay implements PayableCommand {
    private final Money amount;

    public Pay(Money amount) {
      this.amount = java.util.Objects.requireNonNull(amount, "amount는 필수입니다.");
    }

    @Override
    public Money amount() {
      return amount;
    }
  }
}
