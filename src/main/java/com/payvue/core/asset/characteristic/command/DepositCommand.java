package com.payvue.core.asset.characteristic.command;

import com.payvue.characteristic.HasBalance;
import com.payvue.core.asset.characteristic.CapabilityCommand;
import com.payvue.shared.Money;

public interface DepositCommand extends CapabilityCommand {
  Money amount();

  default void apply(HasBalance balance) {
    balance.add(amount());
  }

  final class Deposit implements DepositCommand {
    private final Money amount;

    public Deposit(Money amount) {
      this.amount = java.util.Objects.requireNonNull(amount, "amount는 필수입니다.");
    }

    @Override
    public Money amount() {
      return amount;
    }
  }
}
