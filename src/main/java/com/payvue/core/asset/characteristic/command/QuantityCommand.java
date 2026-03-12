package com.payvue.core.asset.characteristic.command;

import com.payvue.characteristic.Consumable;
import com.payvue.core.asset.characteristic.CapabilityCommand;

public interface QuantityCommand extends CapabilityCommand {

  final class Consume implements QuantityCommand {
    private final int quantity;

    public Consume(int quantity) {
      this.quantity = quantity;
    }

    public int getQuantity() {
      return quantity;
    }

    public void apply(Consumable consumable) {
      consumable.decreaseQuantity(quantity);
    }
  }

  final class Produce implements QuantityCommand {
    private final int quantity;

    public Produce(int quantity) {
      this.quantity = quantity;
    }

    public int getQuantity() {
      return quantity;
    }

    public void apply(Consumable consumable) {
      consumable.increaseQuantity(quantity);
    }
  }
}
